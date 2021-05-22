package dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.regif.AccessType._

class DmaMem(
    addressWidth: Int = 32,
    dataWidth: Int = 32,
    idWidth: Int = 4,
    bufSize: Int = 16,
    selWidth: Int = 4,
    memByteSize: Int = 512
) extends Component {
  val axiConfig = Axi4Config(
    addressWidth = addressWidth, //log2Up(memByteSize)
    dataWidth = dataWidth,
    idWidth = idWidth,
    useLock = false,
    useRegion = false,
    useCache = false,
    useProt = false,
    useQos = false
  )

  val wbConfig = WishboneConfig(
    addressWidth = addressWidth, //log2Up(memByteSize),
    dataWidth = dataWidth,
    selWidth = selWidth
  )

  val dmaConfig = DmaConfig(
    addressWidth = addressWidth,
    burstLen = 8,
    bufDepth = bufSize,
    dataWidth = dataWidth,
    xySizeMax = 256
  )

  val io = new Bundle {
    val wb = slave(
      Wishbone(wbConfig)
    )
    val ctrl = slave(Ctrl())
  }

  /*
  val fifoArea = new Area {
    val fifo = StreamFifo(Bits(dataWidth bits), memByteSize)
    fifo.io.push.valid := False
    fifo.io.push.payload := 0
    fifo.io.pop.ready := False

    io.ctrl.busy := io.wb.STB && ~io.wb.ACK
    io.ctrl.done := io.wb.ACK

    io.wb.ACK := False
    io.wb.DAT_MISO := 0
    when(io.wb.CYC) {
      when(io.wb.WE) {
        fifo.io.push.valid := io.wb.STB
        fifo.io.push.payload := io.wb.DAT_MOSI
        io.wb.ACK := fifo.io.push.ready
      } otherwise {
        io.wb.ACK := fifo.io.pop.valid
        io.wb.DAT_MISO := fifo.io.pop.payload
        fifo.io.pop.ready := io.wb.STB
      }
    }
  }
   */
  val wishbone = Wishbone(wbConfig)
  val wbEmpty = Wishbone(wbConfig)
  wbEmpty.CYC := False
  wbEmpty.STB := False
  wbEmpty.WE := False
  wbEmpty.ADR := 0
  wbEmpty.DAT_MOSI := 0
  wbEmpty.SEL := 0

  val busIfBeginAddr = memByteSize + dataWidth
  val busif = BusInterface(wishbone, (busIfBeginAddr, 100 Byte))
  val SAR_REG = busif.newReg(doc = "DMA src address")
  val DAR_REG = busif.newReg(doc = "DMA dst address")

  val srcAddr = Bits(addressWidth bits)
  val dstAddr = Bits(addressWidth bits)

  val mem = Axi4SharedWishboneOnChipRam(
    axiConfig,
    wbConfig,
    byteCount = memByteSize
  )

  val useBusIf = false
  if (useBusIf) {
    when(io.wb.ADR < busIfBeginAddr) {
      mem.io.wb << io.wb
      wishbone << wbEmpty
    } otherwise {
      wishbone << io.wb
      mem.io.wb << wbEmpty
    }

    srcAddr := SAR_REG.field(addressWidth bits, RW)
    dstAddr := DAR_REG.field(addressWidth bits, RW)
  } else {
    mem.io.wb << io.wb
    wishbone << wbEmpty

    srcAddr := 4091
    dstAddr := 8193
  }

  // val mem = Axi4SharedOnChipRam(
  //   dataWidth = axiConfig.dataWidth,
  //   byteCount = memByteSize,
  //   idWidth = axiConfig.idWidth
  // )

  val dmaArea = new Area {
    val dma = new Dma(dmaConfig)
    mem.io.axi << dma.io.axi.toShared
    dma.io.ctrl <> io.ctrl

    dma.io.param.sar := srcAddr.asUInt
    dma.io.param.dar := dstAddr.asUInt
    dma.io.param.xsize := 2 * dmaConfig.busByteSize // At least twice bus byte size
    dma.io.param.ysize := 1
    dma.io.param.srcystep := 0
    dma.io.param.dstystep := 0
    dma.io.param.llr := 0
    dma.io.param.bf := True
    dma.io.param.cf := True
  }
}

object DmaMem {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(
      new DmaMem()
    ) printPruned ()
  }
}
