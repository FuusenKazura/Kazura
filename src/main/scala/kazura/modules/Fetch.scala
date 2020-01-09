package kazura.modules

import chisel3._
import chisel3.util._
import kazura.models.InstBits
import kazura.stages.IFIn
import kazura.util.Params._

class FetchIO extends Bundle {
  val prev_pc: UInt = Input(UInt(LEN.W))
  val in: IFIn = Input(new IFIn)
  val out: FetchOut = Output(new FetchOut)
}

class FetchOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
}

class Fetch extends Module  {
  val io: FetchIO = IO(new FetchIO)
  val next_pc: UInt = io.prev_pc + 1.U

  io.out.pc := MuxCase(next_pc, Seq(
    // 分岐予測に失敗していた場合
    (io.in.branch_graduated && io.in.branch_mispredicted) -> io.in.restoration_pc,
    // Jump命令をデコードした場合
    io.in.is_jump -> io.in.jump_pc,
    // 先のクロックでデコードした命令がbranch命令の場合(分岐命令をデコードした場合)
    (io.in.predict_enable && io.in.predict) -> io.in.predict_pc,
    // ストールした場合
    io.in.stall -> io.prev_pc
  ))

  printf("pc: %d, next_pc: %d, branch_mispredicted_enable: %d, branch_mispredicted: %d, restore_pc: %d\n",
    io.prev_pc, io.out.pc, io.in.branch_graduated, io.in.branch_mispredicted, io.in.restoration_pc)
}
