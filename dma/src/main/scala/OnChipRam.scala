package dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import spinal.lib.bus.amba4.axi._

case class Axi4SharedWishboneOnChipRam(
    axiConfig: Axi4Config,
    wbConfig: WishboneConfig,
    byteCount: Int,
    arwStage: Boolean = false
) extends Component {
  val io = new Bundle {
    val axi = slave(Axi4Shared(axiConfig))
    val wb = slave(Wishbone(wbConfig))
  }

  val wordCount = byteCount / axiConfig.bytePerWord
  val ram = Mem(axiConfig.dataType, wordCount.toInt)
  val wordRange =
    log2Up(wordCount) + log2Up(axiConfig.bytePerWord) - 1 downto log2Up(
      axiConfig.bytePerWord
    )

  val wbArea = new Area {
    val memRdyReg = Reg(Bool) init (False)
    val wbVld = io.wb.CYC && io.wb.STB
    when(wbVld) { // Memory read or write take 1 cycle
      memRdyReg := True
    } otherwise {
      memRdyReg := False
    }
    io.wb.ACK := memRdyReg

    io.wb.DAT_MISO := ram.readWriteSync(
      address = io.wb.ADR.resized,
      data = io.wb.DAT_MOSI,
      enable = wbVld,
      write = io.wb.WE,
      mask = io.wb.SEL,
      readUnderWrite = dontCare //writeFirst
    )
  }

  val axiArea = new Area {
    val arw =
      if (arwStage) io.axi.arw.s2mPipe().unburstify.m2sPipe()
      else io.axi.arw.unburstify
    val stage0 = arw.haltWhen(arw.write && !io.axi.writeData.valid)
    io.axi.readRsp.data := ram.readWriteSync(
      address = stage0.addr(axiConfig.wordRange).resized,
      data = io.axi.writeData.data,
      enable = stage0.fire,
      write = stage0.write,
      mask = io.axi.writeData.strb
    )
    io.axi.writeData.ready := arw.valid && arw.write && stage0.ready

    val stage1 = stage0.stage
    stage1.ready := (io.axi.readRsp.ready && !stage1.write) || ((io.axi.writeRsp.ready || !stage1.last) && stage1.write)

    io.axi.readRsp.valid := stage1.valid && !stage1.write
    io.axi.readRsp.id := stage1.id
    io.axi.readRsp.last := stage1.last
    io.axi.readRsp.setOKAY()
    if (axiConfig.useRUser) io.axi.readRsp.user := stage1.user

    io.axi.writeRsp.valid := stage1.valid && stage1.write && stage1.last
    io.axi.writeRsp.setOKAY()
    io.axi.writeRsp.id := stage1.id
    if (axiConfig.useBUser) io.axi.writeRsp.user := stage1.user

    io.axi.arw.ready.noBackendCombMerge //Verilator perf
  }
}
