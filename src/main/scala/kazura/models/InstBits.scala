package kazura.models

import chisel3._
import chisel3.util.Cat
import kazura.util.Params._

class InstBits extends Bundle {
  // レジスタ長を変更した際破滅しないよう定義
  // scala.Predef.assert(rd.getWidth == RF.NUM_W)
  // scala.Predef.assert(rs.getWidth == RF.NUM_W)

  val op: UInt = UInt(4.W)
  val rd: UInt = UInt(3.W)
  val rs: UInt = UInt(3.W)
  val disp6u: UInt = UInt(6.W)
  def disp6s: UInt = Cat(Mux(rs(0), "1111_1111_11".U, "0000_0000_00".U), disp6u)
  def imm9u: UInt = Cat(rs, disp6u)
  def imm9s: UInt = Cat(Mux(rs(0), "1111_111".U, "0000_000".U), imm9u)
  def bits: UInt = Cat(op, rd, rs, disp6u)
}

