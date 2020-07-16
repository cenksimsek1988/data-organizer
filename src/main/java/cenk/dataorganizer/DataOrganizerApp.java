package cenk.dataorganizer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import cenk.dataorganizer.service.DataOrganizer;

@SpringBootApplication
public class DataOrganizerApp {
	private static final Logger logger = LoggerFactory.getLogger(DataOrganizerApp.class);
	@Autowired
	private DataOrganizer organizer;

    public static void main(String[] args) {
		SpringApplication.run(DataOrganizerApp.class,args);
	}

	@PostConstruct
	private void start(){
		try {
			organizer.organize();
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
}
