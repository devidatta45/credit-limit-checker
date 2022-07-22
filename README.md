# BETest

## Credit Limit Checker

### Tech Stack: Akka http, Cats, Akka streams, Scala test

### Starting the App

The application can be started from the command line using `sbt` followed
by `run` command. Provided sbt needs to be installed in the system.

Otherwise if using any IDE like Intellij directly `CreditLimitApp` can
be started. 

By doing any of the above the server will start locally on 9000 port.

Then endpoint can be used to get the HTML content from the csv/prn based
on the input.

Endpoint:

`{{host}}:9000/credit-limit?workbook=Workbook2.csv&workbook=Workbook2.prn`

### Running tests

There are few tests in the repository which can be run in the command
line using `sbt` followed by `test`

Otherwise if using any IDE like Intellij directly the test can
be started from the file. 