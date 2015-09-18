Daily e-mail sender.

To start the app do the following:

1) Configuration.
Add a `conf/application.conf` file with these settings:
```
mailer {
  sendTime = "07:00:00"
  receivers = ["foo@bar.com", "bar@foo.com"]
}

mandrill {
  apiKey = "... api key here ..."
  templateName = "... mandrill template here ..."
}

logger {
  root = ERROR

  # Logger used by the framework:
  play = INFO

  # Logger provided to your application:
  application = DEBUG
}
```
2) Start the app and send a request to `localhost:9000` to wire everything up:
```bash
sbt run
# in another terminal
curl localhost:9000
```
