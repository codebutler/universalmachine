package main

import (
	"encoding/binary"
	"fmt"
	"io/ioutil"
	"math/rand"
	"os"
)

const (
	MaskOp  uint32 = 0b11110000000000000000000000000000
	MaskRa  uint32 = 0b00000000000000000000000111000000
	MaskRb  uint32 = 0b00000000000000000000000000111000
	MaskRc  uint32 = 0b00000000000000000000000000000111
	MaskOrv uint32 = 0b00000001111111111111111111111111
	MaskOra uint32 = 0b00001110000000000000000000000000

	OpCmov  uint32 = 0
	OpAidx  uint32 = 1
	OpAamd  uint32 = 2
	OpAdd   uint32 = 3
	OpMul   uint32 = 4
	OpDiv   uint32 = 5
	OpNand  uint32 = 6
	OpHlt   uint32 = 7
	OpAlloc uint32 = 8
	OpAbndt uint32 = 9
	OpOutp  uint32 = 10
	OpInpt  uint32 = 11
	OpLoadp uint32 = 12
	OpOrtho uint32 = 13
)

func main() {
	//codex, err := ioutil.ReadFile("../kotlin/src/main/resources/codex.umz")
	codex, err := ioutil.ReadFile("../kotlin/src/main/resources/sandmark.umz")
	if err != nil {
		panic(err)
	}

	prog := make([]uint32, len(codex)/4)
	for i := 0; i < len(codex); i += 4 {
		prog[i/4] = binary.BigEndian.Uint32(codex[i : i+4])
	}

	reg := []uint32{0, 0, 0, 0, 0, 0, 0, 0}
	ram := make(map[uint32][]uint32)
	ids := make(map[uint32]bool)
	pc := 0

	ram[0] = prog

	for {
		inst := ram[0][pc]

		op := (inst & MaskOp) >> 28
		ra := (inst & MaskRa) >> 6
		rb := (inst & MaskRb) >> 3
		rc := inst & MaskRc

		pc++

		switch op {
		case OpCmov:
			if reg[rc] != 0 {
				reg[ra] = reg[rb]
			}
		case OpAidx:
			reg[ra] = ram[reg[rb]][reg[rc]]
		case OpAamd:
			ram[reg[ra]][reg[rb]] = reg[rc]
		case OpAdd:
			reg[ra] = reg[rb] + reg[rc]
		case OpMul:
			reg[ra] = reg[rb] * reg[rc]
		case OpDiv:
			if reg[rc] != 0 {
				reg[ra] = reg[rb] / reg[rc]
			}
		case OpNand:
			reg[ra] = ^(reg[rb] & reg[rc])
		case OpHlt:
			os.Exit(0)
		case OpAlloc:
			var id uint32
			if len(ids) > 0 {
				for k := range ids {
					id = k
					break
				}
				delete(ids, id)
			} else {
				id = rand.Uint32()
			}
			ram[id] = make([]uint32, reg[rc])
			reg[rb] = id
		case OpAbndt:
			ids[reg[rc]] = true
		case OpOutp:
			print(string(reg[rc]))
		case OpInpt:
			b := make([]byte, 1)
			_, err := os.Stdin.Read(b)
			if err != nil {
				panic(err)
			}
			reg[rc] = uint32(b[0])
		case OpLoadp:
			idx := reg[rb]
			if idx != 0 {
				tmp := make([]uint32, len(ram[idx]))
				copy(tmp, ram[idx])
				ram[0] = tmp
			}
			pc = int(reg[rc])
		case OpOrtho:
			orv := (inst & MaskOrv)
			ora := (inst & MaskOra) >> 25
			reg[ora] = orv
		default:
			panic(fmt.Sprintf("unknown op %d", op))
		}
	}
}
