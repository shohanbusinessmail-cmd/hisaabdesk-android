
package com.hisaabdesk.app

import android.app.Application
import com.hisaabdesk.app.core.backup.BackupManager
import com.hisaabdesk.app.core.security.PinManager
import com.hisaabdesk.app.data.local.HisaabDatabase
import com.hisaabdesk.app.data.repository.FinanceRepository

class HisaabApplication : Application() {
    lateinit var db: HisaabDatabase; private set
    lateinit var repository: FinanceRepository; private set
    lateinit var pinManager: PinManager; private set
    lateinit var backupManager: BackupManager; private set
    override fun onCreate() {
        super.onCreate()
        db = HisaabDatabase.get(this)
        repository = FinanceRepository(db)
        pinManager = PinManager(this)
        backupManager = BackupManager(db)
    }
}
