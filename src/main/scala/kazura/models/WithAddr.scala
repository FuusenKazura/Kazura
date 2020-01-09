package kazura.models

import chisel3._
import chisel3.internal.firrtl.Width
import kazura.util.Params._

class WithAddr(val addr_width: Width) extends Bundle {
  val addr: UInt = UInt(addr_width)
  val data: UInt = UInt(LEN.W)
}
