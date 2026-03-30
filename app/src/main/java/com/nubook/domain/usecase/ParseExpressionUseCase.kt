package com.nubook.domain.usecase

import org.mariuszgromada.math.mxparser.Expression

/**
 * 数学表达式解析用例
 * 封装 mXparser 库，将自然数学算式字符串转为计算结果
 * 处理除以零、非法输入等异常情况
 */
class ParseExpressionUseCase {

    /**
     * 解析并计算数学表达式
     * @param expressionStr 用户输入的算式字符串，如 "125.5 + 40 * 2"
     * @return 计算成功返回 Result.success(结果)，失败返回 Result.failure(异常)
     */
    fun execute(expressionStr: String): Result<Double> {
        return try {
            // 去除前后空格
            val trimmed = expressionStr.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("表达式为空"))
            }

            // 替换中文符号为英文符号
            val normalized = trimmed
                .replace('×', '*')
                .replace('÷', '/')
                .replace('（', '(')
                .replace('）', ')')

            // 创建 mXparser 表达式对象
            val expression = Expression(normalized)

            // 检查表达式语法是否合法
            if (!expression.checkSyntax()) {
                return Result.failure(
                    IllegalArgumentException("表达式语法错误: ${expression.errorMessage}")
                )
            }

            // 计算结果
            val result = expression.calculate()

            // 检查是否为 NaN（除以零等情况）
            if (result.isNaN()) {
                return Result.failure(ArithmeticException("计算结果无效（可能存在除以零）"))
            }

            // 检查是否为无穷大
            if (result.isInfinite()) {
                return Result.failure(ArithmeticException("计算结果溢出"))
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 格式化金额显示
     * @param amount 金额数值
     * @return 格式化后的字符串，保留两位小数
     */
    fun formatAmount(amount: Double): String {
        return String.format(java.util.Locale.US, "%.2f", amount)
    }
}
