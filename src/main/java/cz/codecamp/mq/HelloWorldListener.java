package cz.codecamp.mq;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldListener {

    public static class HelloMessage {
        public String hello;

        @Override
        public String toString() {
            return "HelloMessage{" +
                "hello='" + hello + '\'' +
                '}';
        }
    }

    @RabbitListener(bindings = @QueueBinding(
        exchange = @Exchange(value = "", durable = "true"),
        value = @Queue(value = "hello_world", durable = "true"),
        key = "hello_world"
    ))
    public void onHelloMessage(HelloMessage message) {
        System.out.println("got message: " + message);
    }

}
