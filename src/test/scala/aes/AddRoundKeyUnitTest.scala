package aes

import chisel3.iotesters
import chisel3.iotesters.{ ChiselFlatSpec, Driver, PeekPokeTester }

class AddRoundKeyUnitTester(c: AddRoundKey) extends PeekPokeTester(c) {

  def computeAddRoundKey(state_in: Array[Int], roundKey: Array[Int]): Array[Int] = {

    var state_out = new Array[Int](Params.StateLength)

    for (i <- 0 until Params.StateLength) {
      state_out(i) = state_in(i) ^ roundKey(i)
    }

    state_out
  }

  private val aes_ark = c
  var state = Array(0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d, 0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34)
  var roundKey = Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f)

  for (i <- 0 until Params.StateLength) {
    poke(aes_ark.io.state_in(i), state(i))
    poke(aes_ark.io.roundKey(i), roundKey(i))
  }
  step(1)

  state = computeAddRoundKey(state, roundKey)
  println(state.deep.mkString(" "))

  for (i <- 0 until Params.StateLength)
    expect(aes_ark.io.state_out(i), state(i))
}

// Run test with:
// sbt 'testOnly aes.AddRoundKeyTester'
// extend with the option '-- -z verbose' or '-- -z vcd' for specific test

class AddRoundKeyTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for (backendName <- backendNames) {
    "AddRoundKey" should s"execute AES AddRoundKey (with ${backendName})" in {
      Driver(() => new AddRoundKey, backendName) {
        c => new AddRoundKeyUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in the tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new AddRoundKey) {
      c => new AddRoundKeyUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from the test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new AddRoundKey) {
      c => new AddRoundKeyUnitTester(c)
    } should be(true)
  }
}
