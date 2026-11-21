## Задание 1

To-Be архитектура КиноБездны:

[Диаграмма контейнеров](schemas/container/C4_Container.png)

Пока по условиям задачи команда небольшая и специализируется на Go, предпочитаемым стеком для микросервисов является Go/Gin.
При этом по мере роста команды допустимо использование Kotlin/Spring Boot, что является хорошей альтернативой для нагруженных сервисов и асинхронного взаимодействия с БД и REST API.

Допущения, принятые в построении диаграммы для более удобочитаемого вида:

1. Связи от BFF сервисов показаны на примере одного сервиса Mobile BFF, хотя аналогичные связи будут и у других сервисов BFF.
2. Для микросервисов показаны только их связи с хранилищами данных, брокерами сообщений и внешними системами. Если микросервисы не связаны между собой через брокер, значит, они обмениваются данными по REST API.
3. Взаимодействие через Kafka показано только для сервисов оплаты платежей и оформления подписок. В будущем может, например, появиться сервис уведомлений, который будет получать информацию из Kafka о скидках, новинках в каталоге и т.п.

## Задание 2

### 1. Proxy
Для реализации прокси-сервиса [proxy](src/microservices/proxy) (API Gateway) выбран стек Python + FastAPI как наиболее простое решение текущей задачи.
В реальном проекте более эффективно использовать готовые решения такие, как Kong, например.

### 2. Kafka
MVP сервис [events](src/microservices/events) реализован на стеке Kotlin + Spring Boot, так как:

- Kotlin coroutines предоставляют возможность асинхронного взаимодействия по REST API и с брокером
- Spring Boot из коробки предоставляет легковесную и просто конфигурируемую библиотеку для работы с Kafka

Скриншоты результатов тестов:

1. [Результаты тестов](tests/results/local/test-results.png)
2. [Состояние Kafka topics после тестов](tests/results/local/test-kafka-topics-result.png). Полученные в результате прогона тестов сообщения:

   - [movies-topic](tests/results/local/test-kafka-movies-message-result.png)
   - [payment-topic](tests/results/local/test-kafka-payment-message-result.png)
   - [user-topic](tests/results/local/test-kafka-user-message-result.png)

3. [Полученные сообщения](tests/results/local/test-event-service-received-messages.png) сервисом events из Kafka.


## Задание 3

Команда начала переезд в Kubernetes для лучшего масштабирования и повышения надежности. 

### CI/CD

В папке .github/worflows доработан деплой новых сервисов proxy и events в docker-build-push.yml, чтобы api-tests при сборке отрабатывали корректно при отправке коммита в новую ветку (cinema).
В результате сборки:
1. В GitHub registry успешно добавлены [packages](tests/results/github/packages.png) для всех четырёх сервисов.
2. Прогон автотестов в пайплайне успешный: [скрин](tests/results/github/pipeline-test.png).

### Proxy в Kubernetes

Результаты после деплоя сервиса в kubernetes:
1. Сервис доступен по доменному имени и https: [скрин](tests/results/minikube/cinemaabyss-example-com.png)
2. Вызовы /api/movies все уходят на сервис movies: [скрин](tests/results/minikube/kube-get-movies.png)
3. Результаты прогона тестов ```npm run test:kubernetes```:
   
   - все тесты, в т.ч. healthcheck пройдены успешно, так как в ingress.yaml они перенаправлены на сервис proxy (считаем, что если proxy доступен, то и все остальные тоже): [результаты тестов в kube](tests/results/minikube/kube-test-results.png)
   - сервис event-service успешно получает отправленные сообщения: [скрин логов](tests/results/minikube/kube-event-service-messages.png)

## Задание 4
Результаты установки helm:
1. Установка helm: [скрин](tests/results/helm/helm-install.png)
2. Создание новых pod: [скрин](tests/results/helm/helm-pods-creating.png)
3. Pods running: [скрин](tests/results/helm/helm-pods-running.png)
4. Сервис так же доступен по домену и https: [скрин](tests/results/helm/helm-cinemaabyss-example-com.png)


# Задание 5
Тестирование Circuit Breaker:
1. [Конфигурация CB](src/kubernetes/circuit-breaker-config.yaml)
2. [Istio injection](tests/results/istio/istio-injection.png)
3. Результаты тестирования:
   
   - movies: ```kubectl exec -n cinemaabyss deployment/fortio-deploy -c fortio -- fortio load -c 50 -qps 0 -n 500 -loglevel Warning http://movies-service:8081/api/movies```
 
     - [Результат тестирования](tests/results/istio/movies-test.png):
   
       - 112 из 500 успешных запросов (22,4%)
       - 388 из 500 с 503 ошибкой 
   
     - [Статистика](tests/results/istio/movies-stats.png):
    
       - 388 из 500 запросов отклонено: нельзя было поставить в очередь из-за превышения лимита
       - 99 из 500 поставлено в очередь
   - monolith: ```kubectl exec -n cinemaabyss deployment/fortio-deploy -c fortio -- fortio load -c 50 -qps 0 -n 500 -loglevel Warning http://monolith:8080/api/users ```

     - [Результат тестирования](tests/results/istio/monolith-test.png):

         - 149 из 500 успешных запросов (29,8%)
         - 351 из 500 с 503 ошибкой

     - [Статистика](tests/results/istio/monolith-stats.png):

         - 351 из 500 запросов отклонено: нельзя было поставить в очередь из-за превышения лимита
         - 113 из 500 поставлено в очередь

Так же были проведены эксперименты с другими настройками CB:
1. Если поставить очень слабые [настройки](tests/results/istio/experiments/cb-light.png), то все ответы будут успешны, т.е. сервис выдерживает такую нагрузку, если разрешить это настройками CB: [скрин](tests/results/istio/experiments/cb-light-result.png)
2. Если поставить очень строгие [настройки](tests/results/istio/experiments/cb-hard.png) и нагрузку при этом побольше (5000 запросов, 300 одновременно), то всего 327 ответов (6,54%) успешны: [скрин](tests/results/istio/experiments/cb-hard-results.png)

Параметры для CB нужно выбирать так же исходя из метрик самого сервиса (CPU, memory), анализ которых в этом задании не предполагался и не производился.