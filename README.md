# nightingale
Компактный и удобный инструмент для создания общих чатов, связывая разные сервера, например делая во всех серверах лобби один чат. Работает через брокер - Cristalix Core.

Как поставить? 
```groovy
dependencies {
    implementation 'me.func:nightingale-api:1.0.13'
}
```

Как подписаться на канал?
```kotlin
Nightingale
  .subscribe("arcade-lobby") // подписываемся на канал
  .start() // игроки когда пишут сообщения, будут автоматически писать в данный канал
```

Чтобы не зависить от сервера можно использовать режим Player To Player, но будет больше лишнего траффика.
```kotlin
Nightingale
  .subscribe("arcade-lobby") // подписываемся на канал
  .useP2p() // включаем режим без сервиса
  .start() // игроки когда пишут сообщения, будут автоматически писать в данный канал
```

Если вы хотите кастомизировать отправку и получение сообщений используете `startCustom(onSend: Consumer<AsyncPlayerChatEvent>, onReceive: Consumer<NightingalePublishMessage>)`

Чтобы сделать глобальное сообщение:
```kotlin
Nightingale.broadcast("arcade-lobby", "Всем привет!") // Напишет на всех серверах, подписавшихся на канал `arcade-lobby`, сообщение
```
