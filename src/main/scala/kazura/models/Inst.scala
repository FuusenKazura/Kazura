package kazura.models

import chisel3._
import kazura.util.Params._

object Inst {
  sealed trait Inst {
    val op: UInt
    val alu_op: UInt
    val is_jump: Bool
    val is_branch: Bool
    val is_halt: Bool
    val rf_w:  Bool
    val mem_r: Bool
    val mem_w: Bool
    val source: Seq[UInt]
    val rs1_use: Bool
    val rs2_use: Bool
  }

  object Nop  extends Inst { val op="b0000".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=false.B; val mem_r=false.B; val mem_w=false.B; val rs1_use=false.B; val rs2_use=false.B; val source=Seq(Source1.ZERO.U,   Source2.ONE.U)}
  object Add  extends Inst { val op="b0001".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.RD.U,     Source2.RS.U)}
  object Sub  extends Inst { val op="b0010".U; val alu_op=ALUOP.SUB.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.RD.U,     Source2.RS.U)}
  object And  extends Inst { val op="b0011".U; val alu_op=ALUOP.AND.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.RD.U,     Source2.RS.U)}
  object Or   extends Inst { val op="b0100".U; val alu_op=ALUOP.OR.U;  val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.RD.U,     Source2.RS.U)}
  object Addi extends Inst { val op="b0101".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=false.B; val source=Seq(Source1.RD.U,     Source2.IMM9S.U)}
  object Subi extends Inst { val op="b0110".U; val alu_op=ALUOP.SUB.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=false.B; val source=Seq(Source1.RD.U,     Source2.IMM9S.U)}
  object Incr extends Inst { val op="b0111".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=false.B; val source=Seq(Source1.RD.U,     Source2.ONE.U)}
  object Decr extends Inst { val op="b1000".U; val alu_op=ALUOP.SUB.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=false.B; val source=Seq(Source1.RD.U,     Source2.ONE.U)}
  object Ldi  extends Inst { val op="b1001".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=true.B;  val mem_r=false.B; val mem_w=false.B; val rs1_use=false.B; val rs2_use=false.B; val source=Seq(Source1.ZERO.U,   Source2.IMM9S.U)}
  object Ld   extends Inst { val op="b1010".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=false.B; val mem_r=true.B;  val mem_w=false.B; val rs1_use=false.B; val rs2_use=true.B;  val source=Seq(Source1.DISP6U.U, Source2.RS.U)}
  object St   extends Inst { val op="b1011".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=false.B; val rf_w=false.B; val mem_r=false.B; val mem_w=true.B;  val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.DISP6U.U, Source2.RS.U)}
  object Beq  extends Inst { val op="b1100".U; val alu_op=ALUOP.BEQ.U;  val is_jump=false.B; val is_branch=true.B;  val is_halt=false.B; val rf_w=false.B; val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.RD.U,     Source2.RS.U)}
  object Bgt  extends Inst { val op="b1101".U; val alu_op=ALUOP.BGT.U;  val is_jump=false.B; val is_branch=true.B;  val is_halt=false.B; val rf_w=false.B; val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=true.B;  val source=Seq(Source1.RD.U,     Source2.RS.U)}
  object Jump extends Inst { val op="b1110".U; val alu_op=ALUOP.ADD.U; val is_jump=true.B;   val is_branch=false.B; val is_halt=false.B; val rf_w=false.B; val mem_r=false.B; val mem_w=false.B; val rs1_use=true.B;  val rs2_use=false.B; val source=Seq(Source1.RD.U,     Source2.IMM9S.U)}
  object Halt extends Inst { val op="b1111".U; val alu_op=ALUOP.ADD.U; val is_jump=false.B;  val is_branch=false.B; val is_halt=true.B;  val rf_w=false.B; val mem_r=false.B; val mem_w=false.B; val rs1_use=false.B; val rs2_use=false.B; val source=Seq(Source1.ZERO.U,   Source2.ONE.U)}
}
