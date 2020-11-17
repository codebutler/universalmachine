@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.codebutler.universalmachine

import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.system.exitProcess

const val MASK_OP  = 0b11110000000000000000000000000000u
const val MASK_RA  = 0b00000000000000000000000111000000u
const val MASK_RB  = 0b00000000000000000000000000111000u
const val MASK_RC  = 0b00000000000000000000000000000111u
const val MASK_ORV = 0b00000001111111111111111111111111u
const val MASK_ORA = 0b00001110000000000000000000000000u

const val OP_CMOV  = 0u
const val OP_AIDX  = 1u
const val OP_AAMD  = 2u
const val OP_ADD   = 3u
const val OP_MUL   = 4u
const val OP_DIV   = 5u
const val OP_NAND  = 6u
const val OP_HLT   = 7u
const val OP_ALLOC = 8u
const val OP_ABNDT = 9u
const val OP_OUTP  = 10u
const val OP_INPT  = 11u
const val OP_LOADP = 12u
const val OP_ORTHO = 13u

val classLoader: ClassLoader = {}::class.java.classLoader

@OptIn(ExperimentalStdlibApi::class)
fun main() {
//    val codex = classLoader.getResource("codex.umz")!!.readBytes()
     val codex = classLoader.getResource("sandmark.umz")!!.readBytes()
    // val codex = classLoader.getResource("codex.um")!!.readBytes()

    if ((codex.size % 4) != 0) {
        throw Error("bad size ${codex.size} ${codex.size % 4}")
    }

    val reg = UIntArray(8)
    val ram = mutableMapOf<UInt, UIntArray>()
    val ids = mutableListOf<UInt>()
    var pc = 0u

    ram[0u] = run {
        val prog = UIntArray(codex.size / 4)
        for (i in codex.indices step 4) {
            prog[i / 4] = codex.getUIntAt(i)
        }
        prog
    }

    while (true) {
        val inst = ram[0u]!![pc]

        val op = ((inst and MASK_OP) shr 28)
        val ra = ((inst and MASK_RA) shr 6)
        val rb = ((inst and MASK_RB) shr 3)
        val rc = ((inst and MASK_RC))

        pc++

        when (op) {
            OP_CMOV -> {
                if (reg[rc] != 0u) {
                    reg[ra] = reg[rb]
                }
            }
            OP_AIDX -> {
                reg[ra] = ram[reg[rb]]!![reg[rc]]
            }
            OP_AAMD -> {
                ram[reg[ra]]!![reg[rb]] = reg[rc]
            }
            OP_ADD -> {
                reg[ra] = reg[rb] + reg[rc]
            }
            OP_MUL -> {
                reg[ra] = reg[rb] * reg[rc]
            }
            OP_DIV -> {
                if (reg[rc] != 0u) {
                    reg[ra] = reg[rb] / reg[rc]
                }
            }
            OP_NAND -> {
                reg[ra] = (reg[rb] and reg[rc]).inv()
            }
            OP_HLT -> {
                exitProcess(0)
            }
            OP_ALLOC -> {
                val id = ids.removeLastOrNull() ?: Random.nextUInt()
                ram[id] = UIntArray(reg[rc].toInt())
                reg[rb] = id
            }
            OP_ABNDT -> {
                ids.add(reg[rc])
            }
            OP_OUTP -> {
                print(reg[rc].toInt().toChar())
            }
            OP_INPT -> {
                val c = System.`in`.read()
                reg[rc] = c.toUInt()
            }
            OP_LOADP -> {
                val idx = reg[rb]
                if (idx != 0u) {
                    ram[0u] = ram[idx]!!.copyOf()
                }
                pc = reg[rc]
            }
            OP_ORTHO -> {
                val orv = (inst and MASK_ORV)
                val ora = ((inst and MASK_ORA) shr 25)
                reg[ora] = orv
            }
            else -> throw IllegalArgumentException("unknown opcode: $op")
        }
    }
}

operator fun UIntArray.get(index: UInt) = this[index.toInt()]
operator fun UIntArray.set(index: UInt, value: UInt) = this.set(index.toInt(), value)

fun ByteArray.getUIntAt(idx: Int) =
        ((this[idx].toUInt() and 0xFFu) shl 24) or
                ((this[idx + 1].toUInt() and 0xFFu) shl 16) or
                ((this[idx + 2].toUInt() and 0xFFu) shl 8) or
                (this[idx + 3].toUInt() and 0xFFu)
