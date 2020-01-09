package kazura.stages

import chisel3._
import chisel3.util._
import kazura.models.WithAddr
import kazura.util.Params._

class IMIO extends Bundle {
  val rd_addr: UInt = Input(UInt(log2Ceil(RF.NUM).W))
  val read: Valid[UInt] = Input(Valid(UInt(log2Ceil(LEN).W)))
  val write: Valid[WithAddr] = Input(Valid(new WithAddr(log2Ceil(MEM.NUM).W)))
  val out: Valid[WithAddr]  = Output(Valid(new WithAddr(RF.NUM_W.W)))
}

class IM extends Module {
  val io: IMIO = IO(new IMIO)
  val mem: SyncReadMem[UInt] = SyncReadMem(MEM.NUM, UInt(LEN.W))

  io.out.valid := io.read.valid
  io.out.bits.addr := io.rd_addr
  io.out.bits.data := mem.read(io.read.bits, io.read.valid)
  when (io.write.valid) {
    mem.write(io.write.bits.addr, io.write.bits.data)
  }
}
