package kazura.models

import chisel3._
import kazura.util.Params._

class InstInfo extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val ctrl: Ctrl = new Ctrl
}
