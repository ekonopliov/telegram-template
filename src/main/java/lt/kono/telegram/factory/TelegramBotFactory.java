package lt.kono.telegram.factory;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import lombok.extern.slf4j.Slf4j;
import lt.kono.telegram.domain.User;
import lt.kono.telegram.repositories.UserRepository;
import lt.kono.telegram.services.ExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;


/**
 * Telegram bot factory
 * All response behavior is declared here
 *
 * Returns singleton component
 */
@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TelegramBotFactory{

    @Value("${telegram.bot.token}")
    private String telegramBotToken;  //Gets token from resources/application.yml file

    private TelegramBot bot;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExampleService exampleService;

    /**
     * Initiates when application context is created (on booting)
     */
    @PostConstruct
    private void init(){

        bot =  new TelegramBot(telegramBotToken);
        bot.setUpdatesListener(new UpdatesListener() {

            /**
             * Method is triggered every time when new update is received
             */
            @Override
            public int process(List<Update> updates) {

                updates.forEach(update -> {
                    //Retrieving all information about update
                    Long chatId = update.message().chat().id();
                    String messageText = update.message().text();
                    String firstName = update.message().chat().firstName();
                    String lastName = update.message().chat().lastName();

                    //Retrieving (or saving new, if not found) user information from database
                    User user = userRepository.findById(chatId).orElseGet(() -> userRepository.save(User.builder()
                            .id(chatId)
                            .firstName(firstName)
                            .lastName(lastName)
                            .build()));

                    //Checking whether message has a photo
                    if(update.message().photo() != null){
                        PhotoSize photo = update.message().photo()[2];  //Method returns 3 sizes of the same photo every time.
                                                                        //Selecting the highest resolution
                        String filePath = getFilePath(photo.fileId());
                        log.info("Received photo: " + filePath);
                    }

                    //Checking whether message has a document
                    if(update.message().document() != null){
                        String filePath = getFilePath(update.message().document().thumb().fileId());
                        log.info("Received file: " + filePath);
                    }

                    //Checking whether message has just text
                    if(messageText != null){
                        log.info("Received text: " + messageText);
                        //Filtering received command text
                        switch (messageText){
                            case "/start":
                                bot.execute(new SendMessage(user.getId(), "**/start** command received. All set <3")
                                        .parseMode(ParseMode.Markdown));
                                break;
                            case "/service":
                                String serviceResponse = exampleService.exampleMethod();
                                bot.execute(new SendMessage(user.getId(), serviceResponse));
                                break;

                            default:
                                bot.execute(new SendMessage(user.getId(),
                                        "Oh, that's something new. I do not know this command yet"));
                        }
                    }
                });

                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
        });
    }

    /**
     * Retrieves absolute file path
     * @param fileId
     * @return absolute file path url
     */
    private String getFilePath(String fileId){
        GetFile request = new GetFile(fileId);
        GetFileResponse getFileResponse = bot.execute(request);
        File file = getFileResponse.file();

        return bot.getFullFilePath(file);
    }
}
