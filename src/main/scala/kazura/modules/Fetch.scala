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
  val branch_mispredicted_enable: Bool = Bool() // 分岐命令の演算が完了したか(完了した演算が分岐であるか)
  val restoration_pc: UInt = UInt(LEN.W)

  val prev_pc: UInt = UInt(LEN.W)
  val prev_total_cnt: UInt = UInt(LEN.W)
  val is_branch: Bool = Bool()
  val is_jump: Bool = Bool()
  val alu_out: UInt = UInt(LEN.W)
  val stall: Bool = Bool()
}
class FetchOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
}

class Fetch extends Module  {
  val io: FetchIO = IO(new FetchIO)
  io.out.total_cnt := io.in.prev_total_cnt + 1.U
  when (io.in.branch_mispredicted_enable && io.in.branch_mispredicted) { // 分岐予測に失敗していた場合
    io.out.pc := io.in.restoration_pc
  } .elsewhen(io.in.predict_enable && io.in.predict) { // 分岐予測する場合(先のクロックでデコードした命令がbranch命令の場合)
    io.out.pc := io.in.predict_pc
  } .otherwise {
    io.out.pc := io.in.prev_pc.+(MuxCase(1.U, Seq(
      io.in.is_jump   -> io.in.alu_out,
      io.in.is_branch -> io.in.alu_out,
      io.in.stall     -> 0.U)))
  }
}
