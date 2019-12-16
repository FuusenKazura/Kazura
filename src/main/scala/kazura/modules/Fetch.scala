package kazura.modules

import chisel3._
import kazura.util.Params._

class IFIO extends Bundle {
  val is_branch_or_jump_detected: Bool = Input(Bool())
  val alu_out: UInt = Input(UInt(LEN.W))
  val stole: Bool = Input(Bool())
  val out: IFOut = Output(new IFOut)
}
class IFOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
}

class Fetch extends Module  {
  val io: IFIO = IO(new IFIO)
}
