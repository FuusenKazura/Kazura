package modules

import chisel3.{Bool, UInt}
import chisel3.iotesters.PeekPokeTester
import kazura.modules.Fetch
import kazura.util.Params.LEN

abstract class FetchUnitTestHelper(m: Fetch) extends PeekPokeTester(m) {
  def setPredict(predict: Boolean, predict_enable: Boolean, predict_pc: Int, branch_mispredicted: Boolean, branch_mispredicted_enable: Boolean, restoration_pc: Int): Unit = {
    poke(m.io.in.predict, predict)
    poke(m.io.in.predict_enable, predict_enable)
    poke(m.io.in.predict_pc, predict_pc)
    poke(m.io.in.branch_mispredicted, branch_mispredicted)
    poke(m.io.in.branch_graduated, branch_mispredicted_enable)
    poke(m.io.in.restoration_pc, restoration_pc)
  }
}

class FetchUnitTester(m: Fetch) extends FetchUnitTestHelper(m) {
  setPredict(false, false, 0, false, false, 0)
  poke(m.io.in.prev_pc, 0)
  poke(m.io.in.alu_out, 127)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 1)

  step(1) // ---------------------------------------------
  // stall test

  setPredict(false, false, 0, false, false, 0)
  poke(m.io.in.prev_pc, 1)
  poke(m.io.in.alu_out, 63)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, true)

  expect(m.io.out.pc, 1)

  step(1) // ---------------------------------------------
  // branch test

  setPredict(false, false, 0, false, false, 0)
  poke(m.io.in.prev_pc, 1)
  poke(m.io.in.alu_out, 15)
  poke(m.io.in.is_branch, true)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 16)

  step(1) // ---------------------------------------------

  setPredict(false, false, 0, false, false, 0)
  poke(m.io.in.prev_pc, 16)
  poke(m.io.in.alu_out, 31)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 17)

  step(1) // ---------------------------------------------
  // jump test

  setPredict(false, false, 0, false, false, 0)
  poke(m.io.in.prev_pc, 17)
  poke(m.io.in.alu_out, 10)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, true)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 27)

  step(1) // ---------------------------------------------
  // 分岐予測を失敗したことが判明時

  setPredict(true, true, 0, true, true, 100)
  poke(m.io.in.prev_pc, 0)
  poke(m.io.in.alu_out, 127)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 100, "分岐予測失敗からのリストア")

  step(1) // ---------------------------------------------
  // 分岐予測が有効な場合

  setPredict(true, true, 200, false, false, 0)
  poke(m.io.in.prev_pc, 1)
  poke(m.io.in.alu_out, 63)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, true)

  expect(m.io.out.pc, 200, "分岐予測")
}
