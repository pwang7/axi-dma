package dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
// import spinal.core.sim._
// import spinal.lib.sim._
// import spinal.sim._

class WBWrapper(
    addressWidth: Int = 32,
    dataWidth: Int = 32,
    selWidth: Int = 4,
    pipeDepth: Int = 32
) extends Component {
  val io = new Bundle {
    val wb = slave(
      Wishbone(
        WishboneConfig(
          addressWidth = addressWidth,
          dataWidth = dataWidth,
          selWidth = selWidth
        )
      )
    )
    val ctrl = slave(Ctrl())
  }

  io.wb.ACK := False

  val sel = io.wb.SEL

/*
  val fifoArea = new Area {
    val fifo = StreamFifo(Bits(dataWidth bits), pipeDepth)
    fifo.io.push.valid := False
    fifo.io.push.payload := 0
    fifo.io.pop.ready := False

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
  val dmaArea = new Area {
    val dmaConfig = DmaConfig(
      addressWidth = addressWidth,
      burstLen = 8,
      bufDepth = pipeDepth,
      dataWidth = dataWidth,
      xySizeMax = 256
    )
    val dma = new Dma(dmaConfig)

    dma.io.ctrl <> io.ctrl

    dma.io.param.sar := io.wb.ADR
    dma.io.param.dar := io.wb.ADR
    dma.io.param.xsize := 2 * dmaConfig.busByteSize // At least twice bus byte size
    dma.io.param.ysize := 1
    dma.io.param.srcystep := 0
    dma.io.param.dstystep := 0
    dma.io.param.llr := 0
    dma.io.param.bf := True
    dma.io.param.cf := True

    dma.io.axi.b.id := dma.io.axi.aw.id
    dma.io.axi.r.id := dma.io.axi.ar.id

    dma.io.axi.ar.ready := False
    dma.io.axi.r.valid := False
    dma.io.axi.r.resp := 0 // OK
    dma.io.axi.r.data := io.wb.DAT_MOSI
    dma.io.axi.r.last := True
    dma.io.axi.aw.ready := False
    dma.io.axi.w.ready := False
    io.wb.DAT_MISO := dma.io.axi.w.data
    dma.io.axi.b.valid := False
  }
}

object WBWrapper {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(
      new WBWrapper(
        addressWidth = 32,
        dataWidth = 32,
        selWidth = 4,
        pipeDepth = 16
      )
    ) printPruned ()
  }
}
