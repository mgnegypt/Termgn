package studio.mgn.economy

/** Every user-facing string on the economy screen; built by the app layer. */
data class EconomyStrings(
    val title: String,
    val dialsTitle: String,
    val taxLabel: String,
    val militaryLabel: String,
    val subsidyLabel: String,
    val incomeLabel: String,
    val expensesLabel: String,
    val netLabel: String,
    val previewLabel: String,
    val confirmDials: String,
    val investTitle: String,
    val investBlocked: String,
    val loanTitle: String,
    val loanAmountHint: String,
    val borrowAction: String,
    val repayAction: String,
    val debtLabel: String,
    val interestLabel: String,
    val debtWarning: String,
    val chartTitle: String,
    val chartEmpty: String,
    val percentSuffix: String,
    val keyLabels: Map<String, String>,
)
