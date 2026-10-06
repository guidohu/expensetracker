package com.github.guidohu.expensetracker.data

/** A no-spend-streak length (in days) worth congratulating, and how to say it. */
data class NoSpendMilestone(val days: Int, val label: String)

/** Checked longest-first so a check that's gone unrun for a while reports the highest one reached. */
val NoSpendMilestones: List<NoSpendMilestone> = listOf(
    NoSpendMilestone(365, "a year"),
    NoSpendMilestone(180, "6 months"),
    NoSpendMilestone(90, "3 months"),
    NoSpendMilestone(60, "2 months"),
    NoSpendMilestone(30, "a month"),
    NoSpendMilestone(21, "3 weeks"),
    NoSpendMilestone(14, "2 weeks"),
    NoSpendMilestone(7, "a week"),
    NoSpendMilestone(2, "2 days"),
    NoSpendMilestone(1, "a day"),
)
