package cz.codecamp.mq;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class TwitterClient implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterClient.class);

    @Value("${twitter.enabled}")
    boolean enabled;

    @Value("${twitter.consumer_key}")
    String consumerKey;

    @Value("${twitter.consumer_secret}")
    String consumerSecret;

    @Value("${twitter.token}")
    String token;

    @Value("${twitter.secret}")
    String secret;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(100000);
    private BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(1000);
    private Thread thread;
    private Client hosebirdClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!enabled) {
            LOGGER.info(getClass().getName() + " not enabled");
            return;
        }

        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);

        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        hosebirdEndpoint.trackTerms(Arrays.asList("#ElectionNight"));

//        StatusesSampleEndpoint hosebirdEndpoint = new StatusesSampleEndpoint();
//
//        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
//
//        hosebirdEndpoint.locations(Collections.singletonList(new Location(
//            new Location.Coordinate(14.208651, 49.936338),
//            new Location.Coordinate(14.707842, 50.176160)
//        )));

        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);
        ClientBuilder builder = new ClientBuilder()
            .name(getClass().getSimpleName())
            .hosts(hosebirdHosts)
            .authentication(hosebirdAuth)
            .endpoint(hosebirdEndpoint)
            .processor(new StringDelimitedProcessor(msgQueue))
            .eventMessageQueue(eventQueue);

        hosebirdClient = builder.build();

        hosebirdClient.connect();

        thread = new Thread(() -> {
            while (!hosebirdClient.isDone()) {
                try {
                    String message = msgQueue.take();

                    MessageProperties messageProperties = new MessageProperties();
                    messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    messageProperties.setContentEncoding("UTF-8");
                    rabbitTemplate.send(
                        "",
                        "election_night",
                        new Message(message.getBytes(), messageProperties)
                    );

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        thread.start();
    }

    @Override
    public void destroy() throws Exception {
        if (hosebirdClient != null) {
            hosebirdClient.stop();
            hosebirdClient = null;
        }

        if (thread != null) {
            try {
                thread.interrupt();
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread = null;
        }
    }

}
