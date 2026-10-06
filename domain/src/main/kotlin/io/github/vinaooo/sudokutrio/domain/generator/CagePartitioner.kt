package io.github.vinaooo.sudokutrio.domain.generator

import io.github.vinaooo.sudokutrio.domain.model.Cage
import io.github.vinaooo.sudokutrio.domain.model.Grid
import kotlin.random.Random

/**
 * Splits a solved board into Killer cages: random groups of side-by-side cells, mostly 2 to 4, with no digit twice.
 * A cell left alone joins a neighboring cage when it can, so one-cell cages are rare.
 */
object CagePartitioner {
    private const val MIN_SIZE = 2
    private const val MAX_SIZE = 4
    private const val MAX_MERGED_SIZE = 5

    fun partition(solution: List<Int>, random: Random): List<Cage> {
        val cageOf = IntArray(Grid.SIZE) { -1 }
        val groups = mutableListOf<MutableList<Int>>()
        Grid.CELLS.shuffled(random).filter { cageOf[it] == -1 }.forEach { start ->
            if (cageOf[start] != -1) return@forEach
            val group = mutableListOf(start).also { groups += it }
            cageOf[start] = groups.lastIndex
            val size = random.nextInt(MIN_SIZE, MAX_SIZE + 1)
            while (group.size < size) {
                val next = group.flatMap(::neighbors)
                    .filter { cageOf[it] == -1 && group.none { cell -> solution[cell] == solution[it] } }
                    .distinct().randomOrNull(random) ?: break
                group += next
                cageOf[next] = groups.lastIndex
            }
        }
        groups.filter { it.size == 1 }.forEach { single -> mergeIntoNeighbor(single, groups, cageOf, solution) }
        return groups.filter { it.isNotEmpty() }.map { cells -> Cage(cells.sumOf { solution[it] }, cells.sorted()) }
    }

    private fun mergeIntoNeighbor(
        single: MutableList<Int>,
        groups: List<MutableList<Int>>,
        cageOf: IntArray,
        solution: List<Int>,
    ) {
        val cell = single.single()
        val target = neighbors(cell).map { groups[cageOf[it]] }.firstOrNull { group ->
            group !== single && group.size < MAX_MERGED_SIZE && group.none { solution[it] == solution[cell] }
        } ?: return
        target += cell
        cageOf[cell] = groups.indexOf(target)
        single.clear()
    }

    private fun neighbors(cell: Int): List<Int> {
        val row = Grid.row(cell)
        val column = Grid.column(cell)
        return listOfNotNull(
            Grid.cell(row - 1, column).takeIf { row > 0 },
            Grid.cell(row + 1, column).takeIf { row < Grid.SIDE - 1 },
            Grid.cell(row, column - 1).takeIf { column > 0 },
            Grid.cell(row, column + 1).takeIf { column < Grid.SIDE - 1 },
        )
    }
}
