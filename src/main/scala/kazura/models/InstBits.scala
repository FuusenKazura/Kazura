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
  def disp6s: UInt = Cat(Mux(disp6u.asTypeOf(new Disp6U).h, "b1111_1111_11".U, "b0000_0000_00".U), disp6u)
  def imm9u: UInt = Cat(rs, disp6u)
  def imm9s: UInt = Cat(Mux(imm9u.asTypeOf(new Imm9U).h, "b1111_111".U, "b0000_000".U), imm9u)
  def bits: UInt = Cat(op, rd, rs, disp6u)
}

// chisel3の先頭bitはこっちであっている？
class Disp6U extends Bundle {
  val h: Bool = Bool()
  val t: UInt = UInt(5.W)
}

class Imm9U extends Bundle {
  val h: Bool = Bool()
  val t: UInt = UInt(8.W)
}
