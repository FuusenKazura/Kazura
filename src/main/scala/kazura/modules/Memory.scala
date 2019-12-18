package kazura.modules

import chisel3._
import kazura.util.Params._

class MemoryIO extends Bundle {
  val alu_out: UInt = Input(UInt(LEN.W))
  val in: MemoryIn = Input(new MemoryIn)
  val out: UInt = Output(UInt(LEN.W))
}

class MemoryIn extends Bundle {
  val mem_r: Bool = Bool()
  val mem_w: Bool = Bool()
  val rd_addr: UInt = UInt(RF.NUM_W.W)
}

class Memory extends Module {
  val io = IO(new MemoryIO)
}
