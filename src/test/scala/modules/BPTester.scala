package modules

import chisel3.iotesters._
import kazura.modules.BranchPredictor

class BPTester(m: BranchPredictor) extends PeekPokeTester(m) {
  def set(pc: Int, learning_valid: Boolean, learning_pc: Int, learning_result: Boolean): Unit = {
    poke(m.io.pc, pc)
    poke(m.io.learning.valid, learning_valid)
    poke(m.io.learning.bits.pc, learning_pc)
    poke(m.io.learning.bits.result, learning_result)
    poke(m.io.stall, false)
  }

  // 初期値調整
  for(_ <- 0 until 4) {
    set(15, true, 0, true)
    step(1)
  }
  for(_ <- 0 until 4) {
    set(15, true, 1, false)
    step(1)
  }
  for(_ <- 0 until 4) {
    set(15, true, 2, true)
    step(1)
  }
  for(_ <- 0 until 4) {
    set(15, true, 3, false)
    step(1)
  }

  set(0, false, 15, false)
  step(1)

  expect(m.io.predict, true, "A")
  set(1, false, 15, false)
  step(1)

  expect(m.io.predict, false, "B")
  set(2, false, 15, false)
  step(1)

  expect(m.io.predict, true, "C")
  set(3, false, 15, false)
  step(1)

  expect(m.io.predict, false, "D")

  // for(_ <- 0 until 4) {
  //   set(0, true, 0, true)
  //   step(1)
  // }
  // set(0, true, 0, false)
  // expect(m.io.predict, true, "0")
  // step(1)

  // set(0, true, 0, false)
  // expect(m.io.predict, true, "1")
  // step(1)

  // set(0, true, 0, false)
  // expect(m.io.predict, false, "2")
  // step(1)

  // set(1, true, 0, false)
  // expect(m.io.predict, false, "3")
  // step(1)

  // expect(m.io.predict, true, "4")
}
