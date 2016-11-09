package cz.codecamp.controller;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublishController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @RequestMapping(method = RequestMethod.POST, path = "/publish/{routingKey}")
    public String publish(@PathVariable("routingKey") String routingKey,
                          @RequestBody String body) {

        return publish("", routingKey, body);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/publish/{exchange}/{routingKey}")
    public String publish(@PathVariable("exchange") String exchange,
                          @PathVariable("routingKey") String routingKey,
                          @RequestBody String body) {

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding("UTF-8");
        rabbitTemplate.send(
            exchange,
            routingKey,
            new Message(body.getBytes(), messageProperties)
        );

        return "ok\n";
    }

}
