package kazura.modules

import chisel3._
import kazura.util.Params._

class MemoryIO extends Bundle {
  val mem_r: Bool = Input(Bool())
  val mem_w: Bool = Input(Bool())
  val address: UInt = Input(UInt(LEN.W))
  val in_data: UInt = Input(UInt(LEN.W))
  val out_data: UInt = Output(UInt(LEN.W))
}

class Memory(val init_data: Seq[UInt]) extends Module {
  val io: MemoryIO = IO(new MemoryIO)

  val out_data: UInt = Wire(UInt(LEN.W))
  out_data := 0.U

  // DEBUG_MODEの場合ダミーデータを使う
  if (DEBUG_MODE) {
    val mem: Vec[UInt] = RegInit(VecInit(init_data))

    when (io.mem_w) {
      mem(io.address) := io.in_data
    } .elsewhen(io.mem_r) {
      out_data := mem(io.address)
    }

    printf("mem: ")
    for (i <- mem.indices)
      printf("%d ", mem(i))
    printf("\n")
  } else {
    val mem: Mem[UInt] = Mem(256, UInt(LEN.W))

    when (io.mem_w) {
      mem.write(io.address, io.in_data)
    } .elsewhen(io.mem_r) {
      out_data := mem.read(io.address)
    }
  }

  io.out_data := RegNext(out_data)
}
