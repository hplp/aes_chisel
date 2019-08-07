package aes

import chisel3._
import chisel3.util._

// implements AES_Encrypt
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
class Cipher(Nk: Int, SubBytes_SCD: Boolean) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1

  val io = IO(new Bundle {
    val plaintext = Input(Vec(Params.StateLength, UInt(8.W)))
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W)))
    val start = Input(Bool())
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
    val state_out_valid = Output(Bool())
  })

  // Instantiate module objects
  val AddRoundKeyModule = AddRoundKey()
  val SubBytesModule = SubBytes(SubBytes_SCD)
  val ShiftRowsModule = ShiftRows()
  val MixColumnsModule = MixColumns()

  // Internal variables
  val initValues = Seq.fill(Params.StateLength)(0.U(8.W))
  val state = RegInit(VecInit(initValues))
  val rounds = RegInit(0.U(4.W))

  // STM
  val sIdle :: sInitialAR :: sBusy :: Nil = Enum(3)
  val STM = RegInit(sIdle)

  switch(STM) {
    is(sIdle) {
      when(io.start) {
        STM := sInitialAR
      } // Start cipher
      rounds := 0.U
    }
    is(sInitialAR) {
      rounds := rounds + 1.U
      STM := sBusy
    }
    is(sBusy) {
      rounds := rounds + 1.U
      when(rounds === Nr.U) {
        STM := sIdle
      }
    }
  }

  // SubBytes state
  SubBytesModule.io.state_in := state

  // ShiftRows state
  ShiftRowsModule.io.state_in := SubBytesModule.io.state_out

  // MixColumns state
  MixColumnsModule.io.state_in := ShiftRowsModule.io.state_out

  // AddRoundKey state
  AddRoundKeyModule.io.state_in := Mux(STM === sInitialAR, io.plaintext,
    Mux(rounds === Nr.U, ShiftRowsModule.io.state_out, MixColumnsModule.io.state_out))
  AddRoundKeyModule.io.roundKey := io.roundKey

  state := Mux(STM =/= sIdle, AddRoundKeyModule.io.state_out, VecInit(initValues))

  // Set state_out_valid true when cipher ends
  io.state_out_valid := rounds === Nrplus1.U
  io.state_out := Mux(rounds === Nrplus1.U, state, RegInit(VecInit(initValues)))

  // Debug statements
  //  printf("E_STM: %d, rounds: %d, valid: %d\n", STM, rounds, io.state_out_valid)
  //  printf("E_roundKey: %x %x %x %x %x %x %x %x %x %x %x %x %x %x %x %x\n", io.roundKey(0), io.roundKey(1), io.roundKey(2), io.roundKey(3), io.roundKey(4), io.roundKey(5), io.roundKey(6), io.roundKey(7), io.roundKey(8), io.roundKey(9), io.roundKey(10), io.roundKey(11), io.roundKey(12), io.roundKey(13), io.roundKey(14), io.roundKey(15))
  //  printf("state: %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n", state(0), state(1), state(2), state(3), state(4), state(5), state(6), state(7), state(8), state(9), state(10), state(11), state(12), state(13), state(14), state(15))
}

object Cipher {
  def apply(Nk: Int, SubBytes_SCD: Boolean): Cipher = Module(new Cipher(Nk, SubBytes_SCD))
}