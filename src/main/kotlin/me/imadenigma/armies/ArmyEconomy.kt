package me.imadenigma.armies

interface ArmyEconomy {

    fun deposit(amount: Double)

    fun withdraw(amount: Double)

    fun getBalance(): Double

}