package aes

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.Cat

// implements wrapper for AES cipher and inverse cipher
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
class AES(Nk: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1
  val EKDepth: Int = 16 // enough memory for any expanded key

  val io = IO(new Bundle {
    val AES_mode = Input(UInt(2.W)) //  0=00=off, 1=01=expanded key update, 2=10=cipher, 3=11=inverse cipher
    val start = Input(Bool())
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext, roundKey
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Instantiate module objects
  val CipherModule = Cipher(Nk, SubBytes_SCD)
  val InvCipherModule = InvCipher(Nk, InvSubBytes_SCD)

  // Create a synchronous-read, synchronous-write memory block big enough for any key length
  // A roundKey is Params.StateLength bytes, and 1+(10/12/14) (< EKDepth) of them are needed
  val expandedKeyMem = SyncReadMem(EKDepth, UInt((Params.StateLength * 8).W))
  val address = RegInit(0.U(log2Ceil(EKDepth).W))
  val dataOut = RegInit(0.U((Params.StateLength * 8).W))

  when(io.AES_mode === 1.U) {
    expandedKeyMem(address) := Cat(io.input_text(0), io.input_text(1), io.input_text(2), io.input_text(3), io.input_text(4), io.input_text(5), io.input_text(6), io.input_text(7), io.input_text(8), io.input_text(9), io.input_text(10), io.input_text(11), io.input_text(12), io.input_text(13), io.input_text(14), io.input_text(15))
    dataOut := DontCare
    address := address + 1.U
  }
    .otherwise {
      dataOut := expandedKeyMem(address)

      when(io.AES_mode === 2.U) {
        address := address + 1.U
      }
        .elsewhen(io.AES_mode === 3.U) {
          when(address === 0.U) {
            address := Nr.U
          }
            .otherwise {
              address := address - 1.U
            }
        }
        .otherwise {
          address := 0.U
        }
    }

  // The input text can go to both the cipher and the inverse cipher (for now)
  CipherModule.io.plaintext <> io.input_text
  CipherModule.io.roundKey := Array(dataOut(127, 120), dataOut(119, 112), dataOut(111, 104), dataOut(103, 96), dataOut(95, 88), dataOut(87, 80), dataOut(79, 72), dataOut(71, 64), dataOut(63, 56), dataOut(55, 48), dataOut(47, 40), dataOut(39, 32), dataOut(31, 24), dataOut(23, 16), dataOut(15, 8), dataOut(7, 0))

  InvCipherModule.io.ciphertext <> io.input_text
  InvCipherModule.io.roundKey := Array(dataOut(127, 120), dataOut(119, 112), dataOut(111, 104), dataOut(103, 96), dataOut(95, 88), dataOut(87, 80), dataOut(79, 72), dataOut(71, 64), dataOut(63, 56), dataOut(55, 48), dataOut(47, 40), dataOut(39, 32), dataOut(31, 24), dataOut(23, 16), dataOut(15, 8), dataOut(7, 0))

  // Cipher starts at (start=1 and AES_Mode=2)
  CipherModule.io.start := io.start && (io.AES_mode === 2.U)
  // Inverse Cipher starts at (start=1 and AES_Mode=1)
  InvCipherModule.io.start := io.start && (io.AES_mode === 3.U)

  // AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
  io.output_valid := CipherModule.io.state_out_valid || InvCipherModule.io.state_out_valid
  // AES output can be managed using a Mux on the Cipher output and the InvCipher output
  io.output_text <> Mux(CipherModule.io.state_out_valid, CipherModule.io.state_out, InvCipherModule.io.state_out)

  // Debug statements
  //printf("AES mode=%b start=%b, mem_address=%d, mem_dataOut=%x, \n", io.AES_mode, io.start, address, dataOut)
}

object AES {
  def apply(Nk: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean): AES = Module(new AES(Nk, SubBytes_SCD, InvSubBytes_SCD))
}