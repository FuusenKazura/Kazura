package kazura.modules

import chisel3._
import chisel3.util.MuxCase
import kazura.models.InstBits
import kazura.util.Params._

class FetchIO extends Bundle {
  val in: FetchIn = Input(new FetchIn)
  val out: FetchOut = Output(new FetchOut)
}
class FetchIn extends Bundle {
  val predict: Bool = Bool() // 分岐予測の予測
  val predict_enable: Bool = Bool()
  val predict_pc: UInt = UInt(LEN.W)
  val branch_mispredicted: Bool = Bool() // 分岐予測の予測を失敗したか
  val branch_graduated: Bool = Bool() // 分岐命令の演算が完了したか(完了した演算が分岐であるか)
  val restoration_pc: UInt = UInt(LEN.W)

  val is_branch: Bool = Bool()
  val is_jump: Bool = Bool()
  val jump_pc: UInt = UInt(LEN.W)
  val alu_out: UInt = UInt(LEN.W)
  val stall: Bool = Bool()

  val prev_pc: UInt = UInt(LEN.W)
}
class FetchOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
}

class Fetch extends Module  {
  val io: FetchIO = IO(new FetchIO)
  when (io.in.branch_graduated && io.in.branch_mispredicted) { // 分岐予測に失敗していた場合
    io.out.pc := io.in.restoration_pc
  } .elsewhen(io.in.is_jump) {
    io.out.pc := io.in.jump_pc
  } .elsewhen(io.in.predict_enable && io.in.predict) { // 分岐予測する場合(先のクロックでデコードした命令がbranch命令の場合)
    io.out.pc := io.in.predict_pc
  } .otherwise {
    io.out.pc := io.in.prev_pc.+(MuxCase(1.U, Seq(
      io.in.is_branch -> io.in.alu_out,
      io.in.stall     -> 0.U)))
  }
  printf("pc: %d, next_pc: %d, branch_mispredicted_enable: %d, branch_mispredicted: %d, restore_pc: %d\n",
    io.in.prev_pc, io.out.pc, io.in.branch_graduated, io.in.branch_mispredicted, io.in.restoration_pc)
}
