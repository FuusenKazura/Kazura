package kazura.modules

import chisel3._
import kazura.util.Params._

class ALUIO extends Bundle {
  val alu_op = Input(UInt(ALUOP.NUM_W.W))
  val source = Input(Vec(2, UInt(LEN.W)))
  val out = Output(UInt(LEN.W))
}

class ALU extends Module {
  val io: ALUIO = IO(new ALUIO)
}
