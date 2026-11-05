# Banking Transaction & Account Management

## Run

From the repository root, compile and launch the console application:

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
java -cp out com.bank.Main
```

The application stores data in memory for the duration of a run. The demo admin
login is `admin` / `admin123`; set `BANK_ADMIN_USERNAME` and
`BANK_ADMIN_PASSWORD` to override those credentials.

Savings accounts require an initial balance of at least ₹500. Customer
transaction history is limited to accounts owned by the logged-in customer.
