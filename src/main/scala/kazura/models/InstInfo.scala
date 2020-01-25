package kazura.models

import chisel3._
import chisel3.util._
import kazura.util.Params._

class InstInfo extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val rob_addr: UInt = UInt(log2Ceil(ROB.BUF_SIZE).W)
  val ctrl: Ctrl = new Ctrl
}
