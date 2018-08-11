data class RegexIOTest(
        val regex: String,
        val tests: List<TestObject>
)

data class TestObject(
        val input: String,
        val result: Boolean
)