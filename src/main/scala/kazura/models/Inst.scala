package kazura.models

import chisel3._
import chisel3.util.Cat
import kazura.util.Params._

class Inst extends Bundle {
  // レジスタ長を変更した際破滅しないよう定義
  scala.Predef.assert(rd.getWidth == RF.NUM_W)
  scala.Predef.assert(rs.getWidth == RF.NUM_W)

  val op: UInt = UInt(4.W)
  val rd: UInt = UInt(3.W)
  val rs: UInt = UInt(3.W)
  val disp6u: UInt = UInt(6.W)
  def imm9u: UInt = Cat(rs, disp6u)
}

