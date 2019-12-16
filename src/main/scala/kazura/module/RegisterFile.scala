package kazura.module

import chisel3._
import kazura.util.Params._
import kazura.model.Inst

class RFIO extends Bundle {
  val reg_addr: Vec[UInt] = Vec(RF.READ_PORT, UInt(RF.NUM_W.W))
  val in = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val out = Vec(RF.READ_PORT, Output(UInt(LEN.W)))
}

class RFWrite extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val rf_w: Bool = Bool()
  val data: UInt = UInt(LEN.W)
}

class RegisterFile extends Module {
  val io = IO(new RFIO)
}