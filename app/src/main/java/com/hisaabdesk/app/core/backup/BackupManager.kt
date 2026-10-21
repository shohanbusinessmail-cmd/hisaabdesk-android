
package com.hisaabdesk.app.core.backup

import android.util.Base64
import androidx.room.withTransaction
import com.hisaabdesk.app.data.local.HisaabDatabase
import com.hisaabdesk.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** JSON backup with SHA-256 integrity; optional AES-256-GCM password encryption. PIN is never exported. */
class BackupManager(private val db: HisaabDatabase) {
    companion object {
        fun sha256(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        fun encrypt(plain: String, password: String): String {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val key = derive(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            fun b(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
            return "HDENC1:${b(salt)}:${b(iv)}:${b(enc)}"
        }
        fun decrypt(payload: String, password: String): String {
            val parts = payload.trim().split(":"); require(parts.size == 4 && parts[0] == "HDENC1") { "এনক্রিপ্ট করা ফাইলের গঠন সঠিক নয়।" }
            val salt = Base64.decode(parts[1], Base64.DEFAULT); val iv = Base64.decode(parts[2], Base64.DEFAULT); val data = Base64.decode(parts[3], Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
            return String(cipher.doFinal(data), Charsets.UTF_8)
        }
        fun isEncrypted(content: String) = content.trim().startsWith("HDENC1:")
        private fun derive(password: String, salt: ByteArray): SecretKeySpec {
            val spec = PBEKeySpec(password.toCharArray(), salt, 150_000, 256)
            return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val a = db.accountDao(); val l = db.ledgerDao(); val c = db.catalogDao(); val s = db.shopDao(); val d = db.debtDao(); val ln = db.loanDao(); val sb = db.savingsBudgetDao()
        val payload = JSONObject()
        fun <T> arr(list: List<T>, f: (T) -> JSONObject): JSONArray { val j = JSONArray(); list.forEach { j.put(f(it)) }; return j }
        payload.put("accounts", arr(a.allOnce()) { JSONObject().put("id", it.id).put("name", it.name).put("type", it.type).put("institution", it.institution).put("identifier", it.identifier).put("opening", it.openingBalancePoisha).put("createdAt", it.createdAt).put("notes", it.notes).put("archived", it.archived) })
        payload.put("ledger", arr(l.allOnce()) { JSONObject().put("id", it.id).put("groupId", it.groupId).put("accountId", it.accountId).put("type", it.type).put("amount", it.amountPoisha).put("at", it.occurredAt).put("categoryId", it.categoryId ?: JSONObject.NULL).put("counterparty", it.counterparty).put("description", it.description).put("method", it.paymentMethod).put("refTable", it.refTable).put("refId", it.refId ?: JSONObject.NULL).put("note", it.note) })
        payload.put("categories", arr(c.categoriesOnce()) { JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind).put("color", it.colorHex).put("icon", it.iconName).put("archived", it.archived) })
        payload.put("subcategories", arr(c.subcategoriesOnce()) { JSONObject().put("id", it.id).put("categoryId", it.categoryId).put("name", it.name).put("archived", it.archived) })
        payload.put("shops", arr(s.shopsOnce()) { JSONObject().put("id", it.id).put("name", it.name).put("keeper", it.keeperName).put("phone", it.phone).put("address", it.address).put("notes", it.notes).put("archived", it.archived) })
        payload.put("shopPurchases", arr(s.purchasesOnce()) { JSONObject().put("id", it.id).put("shopId", it.shopId).put("at", it.purchasedAt).put("total", it.totalPoisha).put("paidNow", it.paidNowPoisha).put("accountId", it.paymentAccountId ?: JSONObject.NULL).put("categoryId", it.categoryId ?: JSONObject.NULL).put("notes", it.notes) })
        payload.put("shopItems", arr(s.itemsOnce()) { JSONObject().put("id", it.id).put("purchaseId", it.purchaseId).put("product", it.productName).put("qty", it.quantityText).put("unitPrice", it.unitPricePoisha).put("lineTotal", it.lineTotalPoisha) })
        payload.put("shopPayments", arr(s.paymentsOnce()) { JSONObject().put("id", it.id).put("shopId", it.shopId).put("at", it.paidAt).put("amount", it.amountPoisha).put("accountId", it.accountId).put("method", it.method).put("reference", it.reference).put("notes", it.notes) })
        payload.put("persons", arr(d.personsOnce()) { JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone).put("address", it.address).put("notes", it.notes).put("archived", it.archived) })
        payload.put("debts", arr(d.debtsOnce()) { JSONObject().put("id", it.id).put("personId", it.personId).put("direction", it.direction).put("principal", it.principalPoisha).put("startAt", it.startAt).put("expected", it.expectedRepayAt ?: JSONObject.NULL).put("purpose", it.purpose).put("accountId", it.accountId).put("notes", it.notes).put("status", it.status) })
        payload.put("debtPayments", arr(d.paymentsOnce()) { JSONObject().put("id", it.id).put("debtId", it.debtId).put("at", it.paidAt).put("amount", it.amountPoisha).put("accountId", it.accountId).put("method", it.method).put("notes", it.notes) })
        payload.put("savings", arr(sb.savingsOnce()) { JSONObject().put("id", it.id).put("name", it.name).put("institution", it.institution).put("purpose", it.purpose).put("opening", it.openingBalancePoisha).put("createdAt", it.createdAt).put("notes", it.notes).put("archived", it.archived) })
        payload.put("savingsMovements", arr(sb.movementsOnce()) { JSONObject().put("id", it.id).put("savingsId", it.savingsId).put("direction", it.direction).put("amount", it.amountPoisha).put("at", it.occurredAt).put("accountId", it.accountId).put("purpose", it.purpose).put("method", it.method).put("notes", it.notes) })
        payload.put("budgets", arr(sb.budgetsOnce()) { JSONObject().put("id", it.id).put("name", it.name).put("startAt", it.startAt).put("endAt", it.endAt).put("limit", it.limitPoisha).put("categoryId", it.categoryId ?: JSONObject.NULL).put("notes", it.notes) })
        payload.put("recurring", arr(sb.recurringOnce()) { JSONObject().put("id", it.id).put("title", it.title).put("type", it.type).put("amount", it.amountPoisha).put("accountId", it.accountId).put("categoryId", it.categoryId ?: JSONObject.NULL).put("frequency", it.frequency).put("nextDueAt", it.nextDueAt).put("autoPost", it.autoPost).put("active", it.active).put("notes", it.notes) })
        payload.put("ngoLoans", arr(ln.ngoOnce()) { JSONObject().put("id", it.id).put("name", it.ngoName).put("identifier", it.loanIdentifier).put("principal", it.principalPoisha).put("disbursedAt", it.disbursedAt).put("service", it.serviceChargePoisha ?: JSONObject.NULL).put("other", it.otherChargesPoisha ?: JSONObject.NULL).put("totalPayable", it.totalPayablePoisha ?: JSONObject.NULL).put("frequency", it.frequency).put("installment", it.installmentPoisha ?: JSONObject.NULL).put("count", it.installmentCount ?: JSONObject.NULL).put("firstAt", it.firstInstallmentAt ?: JSONObject.NULL).put("accountId", it.disbursementAccountId).put("notes", it.notes).put("status", it.status) })
        payload.put("ngoInstallments", arr(ln.ngoInstOnce()) { JSONObject().put("id", it.id).put("loanId", it.loanId).put("dueAt", it.dueAt).put("expected", it.expectedPoisha) })
        payload.put("ngoPayments", arr(ln.ngoPayOnce()) { JSONObject().put("id", it.id).put("loanId", it.loanId).put("installmentId", it.installmentId ?: JSONObject.NULL).put("at", it.paidAt).put("amount", it.amountPoisha).put("accountId", it.accountId).put("notes", it.notes) })
        payload.put("bankLoans", arr(ln.bankOnce()) { JSONObject().put("id", it.id).put("bank", it.bankName).put("nickname", it.nickname).put("masked", it.maskedIdentifier).put("principal", it.principalPoisha).put("disbursedAt", it.disbursedAt).put("rate", it.annualRatePercent ?: JSONObject.NULL).put("method", it.method).put("tenure", it.tenureMonths ?: JSONObject.NULL).put("frequency", it.frequency).put("installment", it.installmentPoisha ?: JSONObject.NULL).put("fee", it.processingFeePoisha).put("other", it.otherChargesPoisha).put("totalPayable", it.contractualTotalPayablePoisha ?: JSONObject.NULL).put("accountId", it.disbursementAccountId).put("notes", it.notes).put("status", it.status) })
        payload.put("bankSchedule", arr(ln.bankSchedOnce()) { JSONObject().put("id", it.id).put("loanId", it.loanId).put("dueAt", it.dueAt).put("expected", it.expectedPoisha).put("interest", it.interestPoisha ?: JSONObject.NULL).put("principal", it.principalPoisha ?: JSONObject.NULL) })
        payload.put("bankPayments", arr(ln.bankPayOnce()) { JSONObject().put("id", it.id).put("loanId", it.loanId).put("scheduleId", it.scheduleId ?: JSONObject.NULL).put("at", it.paidAt).put("amount", it.amountPoisha).put("interest", it.interestPoisha ?: JSONObject.NULL).put("principal", it.principalPoisha ?: JSONObject.NULL).put("accountId", it.accountId).put("notes", it.notes) })
        val payloadStr = payload.toString()
        JSONObject().put("app", "HisaabDesk").put("backupVersion", 1).put("schemaVersion", 1)
            .put("exportedAt", System.currentTimeMillis()).put("sha256", sha256(payloadStr)).put("payload", payload).toString()
    }

    /** Validates checksum/version FIRST; only then replaces data inside one transaction. Failure leaves DB untouched. */
    suspend fun restore(jsonText: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonText)
        require(root.optString("app") == "HisaabDesk") { "ব্যাকআপ ফাইলটি যাচাই করা যায়নি। বর্তমান ডেটা পরিবর্তন করা হয়নি।" }
        require(root.optInt("backupVersion") == 1 && root.optInt("schemaVersion") == 1) { "এই ব্যাকআপ সংস্করণটি সমর্থিত নয়।" }
        val payload = root.getJSONObject("payload")
        require(sha256(payload.toString()) == root.getString("sha256")) { "ব্যাকআপ ফাইলটি যাচাই করা যায়নি। বর্তমান ডেটা পরিবর্তন করা হয়নি।" }
        fun arr(name: String): JSONArray = payload.optJSONArray(name) ?: JSONArray()
        fun lo(o: JSONObject, k: String): Long? = if (o.isNull(k)) null else o.getLong(k)
        db.withTransaction {
            val a = db.accountDao(); val l = db.ledgerDao(); val c = db.catalogDao(); val s = db.shopDao(); val d = db.debtDao(); val ln = db.loanDao(); val sbd = db.savingsBudgetDao()
            // children first
            s.delItems(); s.delPayments(); s.delPurchases(); s.delShops()
            d.delPayments(); d.delDebts(); d.delPersons()
            ln.delNgoPay(); ln.delNgoInst(); ln.delNgo(); ln.delBankPay(); ln.delBankSched(); ln.delBank()
            sbd.delMovements(); sbd.delSavings(); sbd.delBudgets(); sbd.delRecurring(); sbd.delReminders()
            l.deleteAll(); c.deleteSubcategories(); c.deleteCategories(); a.deleteAll()
            for (i in 0 until arr("accounts").length()) { val o = arr("accounts").getJSONObject(i); a.insert(FinancialAccount(o.getLong("id"), o.getString("name"), o.getString("type"), o.optString("institution"), o.optString("identifier"), o.getLong("opening"), o.getLong("createdAt"), o.optString("notes"), o.optBoolean("archived"))) }
            for (i in 0 until arr("categories").length()) { val o = arr("categories").getJSONObject(i); c.insertCategory(TransactionCategory(o.getLong("id"), o.getString("name"), o.getString("kind"), o.optString("color", "#16834A"), o.optString("icon", "label"), o.optBoolean("archived"))) }
            for (i in 0 until arr("subcategories").length()) { val o = arr("subcategories").getJSONObject(i); c.insertSubcategory(CategorySubcategory(o.getLong("id"), o.getLong("categoryId"), o.getString("name"), o.optBoolean("archived"))) }
            for (i in 0 until arr("ledger").length()) { val o = arr("ledger").getJSONObject(i); l.insert(LedgerEntry(o.getLong("id"), o.getString("groupId"), o.getLong("accountId"), o.getString("type"), o.getLong("amount"), o.getLong("at"), lo(o, "categoryId"), null, o.optString("counterparty"), o.optString("description"), o.optString("method"), o.optString("refTable"), lo(o, "refId"), o.optString("note"))) }
            for (i in 0 until arr("shops").length()) { val o = arr("shops").getJSONObject(i); s.insertShop(Shop(o.getLong("id"), o.getString("name"), o.optString("keeper"), o.optString("phone"), o.optString("address"), "", o.optString("notes"), o.optBoolean("archived"))) }
            for (i in 0 until arr("shopPurchases").length()) { val o = arr("shopPurchases").getJSONObject(i); s.insertPurchase(ShopPurchase(o.getLong("id"), o.getLong("shopId"), o.getLong("at"), o.getLong("total"), o.getLong("paidNow"), lo(o, "accountId"), lo(o, "categoryId"), o.optString("notes"))) }
            val items = mutableListOf<ShopPurchaseItem>(); for (i in 0 until arr("shopItems").length()) { val o = arr("shopItems").getJSONObject(i); items.add(ShopPurchaseItem(o.getLong("id"), o.getLong("purchaseId"), o.getString("product"), o.getString("qty"), o.getLong("unitPrice"), o.getLong("lineTotal"))) }; if (items.isNotEmpty()) s.insertItems(items)
            for (i in 0 until arr("shopPayments").length()) { val o = arr("shopPayments").getJSONObject(i); s.insertPayment(ShopPayment(o.getLong("id"), o.getLong("shopId"), o.getLong("at"), o.getLong("amount"), o.getLong("accountId"), o.optString("method"), o.optString("reference"), o.optString("notes"))) }
            for (i in 0 until arr("persons").length()) { val o = arr("persons").getJSONObject(i); d.insertPerson(Person(o.getLong("id"), o.getString("name"), o.optString("phone"), o.optString("address"), o.optString("notes"), o.optBoolean("archived"))) }
            for (i in 0 until arr("debts").length()) { val o = arr("debts").getJSONObject(i); d.insertDebt(PersonalDebt(o.getLong("id"), o.getLong("personId"), o.getString("direction"), o.getLong("principal"), o.getLong("startAt"), lo(o, "expected"), o.optString("purpose"), o.getLong("accountId"), o.optString("notes"), o.optString("status", "ACTIVE"))) }
            for (i in 0 until arr("debtPayments").length()) { val o = arr("debtPayments").getJSONObject(i); d.insertPayment(PersonalDebtPayment(o.getLong("id"), o.getLong("debtId"), o.getLong("at"), o.getLong("amount"), o.getLong("accountId"), o.optString("method"), o.optString("notes"))) }
            for (i in 0 until arr("savings").length()) { val o = arr("savings").getJSONObject(i); sbd.insertSavings(SavingsAccount(o.getLong("id"), o.getString("name"), o.optString("institution"), o.optString("purpose"), o.getLong("opening"), o.getLong("createdAt"), o.optString("notes"), o.optBoolean("archived"))) }
            for (i in 0 until arr("savingsMovements").length()) { val o = arr("savingsMovements").getJSONObject(i); sbd.insertMovement(SavingsMovement(o.getLong("id"), o.getLong("savingsId"), o.getString("direction"), o.getLong("amount"), o.getLong("at"), o.getLong("accountId"), o.optString("purpose"), o.optString("method"), o.optString("notes"))) }
            for (i in 0 until arr("budgets").length()) { val o = arr("budgets").getJSONObject(i); sbd.insertBudget(Budget(o.getLong("id"), o.getString("name"), o.getLong("startAt"), o.getLong("endAt"), o.getLong("limit"), lo(o, "categoryId"), null, o.optString("notes"))) }
            for (i in 0 until arr("recurring").length()) { val o = arr("recurring").getJSONObject(i); sbd.insertRecurring(RecurringTransaction(o.getLong("id"), o.getString("title"), o.getString("type"), o.getLong("amount"), o.getLong("accountId"), lo(o, "categoryId"), o.getString("frequency"), o.getLong("nextDueAt"), o.optBoolean("autoPost"), o.optBoolean("active", true), o.optString("notes"))) }
            for (i in 0 until arr("ngoLoans").length()) { val o = arr("ngoLoans").getJSONObject(i); ln.insertNgo(NgoLoanAccount(o.getLong("id"), o.getString("name"), o.optString("identifier"), o.getLong("principal"), o.getLong("disbursedAt"), lo(o, "service"), lo(o, "other"), lo(o, "totalPayable"), o.optString("frequency", "WEEKLY"), lo(o, "installment"), if (o.isNull("count")) null else o.getInt("count"), lo(o, "firstAt"), o.getLong("accountId"), o.optString("notes"), o.optString("status", "ACTIVE"))) }
            val ni = mutableListOf<NgoLoanInstallment>(); for (i in 0 until arr("ngoInstallments").length()) { val o = arr("ngoInstallments").getJSONObject(i); ni.add(NgoLoanInstallment(o.getLong("id"), o.getLong("loanId"), o.getLong("dueAt"), o.getLong("expected"))) }; if (ni.isNotEmpty()) ln.insertNgoInstallments(ni)
            for (i in 0 until arr("ngoPayments").length()) { val o = arr("ngoPayments").getJSONObject(i); ln.insertNgoPayment(NgoLoanPayment(o.getLong("id"), o.getLong("loanId"), lo(o, "installmentId"), o.getLong("at"), o.getLong("amount"), o.getLong("accountId"), o.optString("notes"))) }
            for (i in 0 until arr("bankLoans").length()) { val o = arr("bankLoans").getJSONObject(i); ln.insertBank(BankLoanAccount(o.getLong("id"), o.getString("bank"), o.optString("nickname"), o.optString("masked"), o.getLong("principal"), o.getLong("disbursedAt"), if (o.isNull("rate")) null else o.getString("rate"), o.optString("method", "MANUAL"), if (o.isNull("tenure")) null else o.getInt("tenure"), o.optString("frequency", "MONTHLY"), lo(o, "installment"), o.optLong("fee"), o.optLong("other"), lo(o, "totalPayable"), o.getLong("accountId"), o.optString("notes"), o.optString("status", "ACTIVE"))) }
            val bs = mutableListOf<BankLoanSchedule>(); for (i in 0 until arr("bankSchedule").length()) { val o = arr("bankSchedule").getJSONObject(i); bs.add(BankLoanSchedule(o.getLong("id"), o.getLong("loanId"), o.getLong("dueAt"), o.getLong("expected"), lo(o, "interest"), lo(o, "principal"))) }; if (bs.isNotEmpty()) ln.insertBankSchedule(bs)
            for (i in 0 until arr("bankPayments").length()) { val o = arr("bankPayments").getJSONObject(i); ln.insertBankPayment(BankLoanPayment(o.getLong("id"), o.getLong("loanId"), lo(o, "scheduleId"), o.getLong("at"), o.getLong("amount"), lo(o, "interest"), lo(o, "principal"), o.getLong("accountId"), o.optString("notes"))) }
            Unit
        }
    }

    fun exportCsv(ledger: List<LedgerEntry>): String = buildString {
        appendLine("date,type,amount_poisha,description,counterparty,method")
        ledger.forEach { appendLine("${it.occurredAt},${it.type},${it.amountPoisha},\"${it.description.replace("\"", "")}\",\"${it.counterparty.replace("\"", "")}\",${it.paymentMethod}") }
    }
}
