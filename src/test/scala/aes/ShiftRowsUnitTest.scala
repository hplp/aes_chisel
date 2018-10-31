package aes

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class ShiftRowsUnitTester(c: ShiftRows) extends PeekPokeTester(c) {

  // ShiftRows in Scala
  def computeShiftRows(state_in: Array[Int]): Array[Int] = {
    var state_out = new Array[Int](Params.StateLength)

    state_out(0) = state_in(0)
    state_out(1) = state_in(5)
    state_out(2) = state_in(10)
    state_out(3) = state_in(15)

    state_out(4) = state_in(4)
    state_out(5) = state_in(9)
    state_out(6) = state_in(14)
    state_out(7) = state_in(3)

    state_out(8) = state_in(8)
    state_out(9) = state_in(13)
    state_out(10) = state_in(2)
    state_out(11) = state_in(7)

    state_out(12) = state_in(12)
    state_out(13) = state_in(1)
    state_out(14) = state_in(6)
    state_out(15) = state_in(11)

    state_out
  }

  private val aes_sr = c
  var state = Array(0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d, 0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34)

  for (i <- 0 until Params.StateLength)
    poke(aes_sr.io.state_in(i), state(i))
  step(1)

  // run in Scala
  state = computeShiftRows(state)
  println(state.deep.mkString(" "))

  // match chisel and Scala
  for (i <- 0 until Params.StateLength)
    expect(aes_sr.io.state_out(i), state(i))
}

// Run with:
//    sbt 'testOnly aes.ShiftRowsTester'
// or sbt 'testOnly aes.ShiftRowsTester -- -z verbose'
// or sbt 'testOnly aes.ShiftRowsTester -- -z vcd'

class ShiftRowsTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for (backendName <- backendNames) {
    "ShiftRows" should s"execute AES ShiftRows (with $backendName)" in {
      Driver(() => new ShiftRows, backendName) {
        c => new ShiftRowsUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in the tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new ShiftRows) {
      c => new ShiftRowsUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from the test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new ShiftRows) {
      c => new ShiftRowsUnitTester(c)
    } should be(true)
  }
}
