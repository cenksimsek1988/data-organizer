package cenk.dataorganizer.service;

import static cenk.dataorganizer.util.DOFileUtil.prepareDownloadFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class DataFetcher {
	private static final Object LOCK = new Object();
	@Value("${dataorganizer.data.source.http.url.format:non-provided}")
	private String urlFormat;

	@Value("${dataorganizer.data.source.http.url.args.year.start:-1}")
	private int yearStart;

	@Value("${dataorganizer.data.source.http.url.args.year.end:-1}")
	private int yearEnd;

	@Value("${dataorganizer.data.source.http.url.args.frequency:annual}")
	private String frequency;

	@Value("${dataorganizer.raw.data.folder.path:raw}")
	private String rawDataFolderPath;

	@Autowired
	private DataOrganizer organizer;

	private static final Logger logger = LoggerFactory.getLogger(DataFetcher.class);
	private static final String ANNUAL = "ANNUAL";
	private static final String QUARTERLY = "QUARTERLY";
	private static final String MONTHLY = "MONTHLY";
	private static final OkHttpClient client;
	private int count = 0;
	private int tot = 0;
	static {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.connectTimeout(30, TimeUnit.SECONDS);
		builder.readTimeout(30, TimeUnit.SECONDS);
		builder.writeTimeout(30, TimeUnit.SECONDS);
		builder.retryOnConnectionFailure(true);
		client = builder.build();
	}
	
	private void process() {
		count++;
		logger.debug("process: {}%", 100*count/tot);
		if(count>=tot) {
			done();
		}
	}
	
	private void done() {
		try {
			organizer.organize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int[] populateYears() {
		int[] answer = new int[(yearEnd-yearStart)+1];
		for(int i = 0; i < answer.length; i++) {
			int year = yearStart + i;
			answer[i] = year;
		}
		return answer;
	}

	private void fetchAnnually(int[] years) {
		for(int y:years) {
			String url = String.format(urlFormat, y);
			try {
				fetchData(url);
			} catch (IOException e) {
				logger.error("error while downloading an annual data file");
				logger.error(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}
	
	private class DownloadCallback implements Callback {
		@Override
		public void onFailure(Call call, IOException e) {
			logger.error("error while request:");
			logger.error(call.toString());
			logger.error(e.getLocalizedMessage());
			e.printStackTrace();
			process();
		}
		
		@Override
		public void onResponse(Call call, Response response) throws IOException {
			logger.debug("token got from future redirect receiver");
			File fileToWrite = prepareDownloadFile(response, rawDataFolderPath);
			if (!response.isSuccessful()) {
				throw new IOException("Failed to download file: " + response);
			}
			FileOutputStream fos = new FileOutputStream(fileToWrite);
			fos.write(response.body().bytes());
			fos.close();
			process();
		}
		
	}

	private void fetchData(String url) throws IOException {
		tot++;
		HttpUrl finalUrl = HttpUrl.parse(url).newBuilder().build();
		logger.debug("url: {}", finalUrl);
		Request request = new Request.Builder().url(finalUrl).addHeader("charset", "utf-8").build();
		final DownloadCallback cb = new DownloadCallback();
		client.newCall(request).enqueue(cb);
	}
	
	private void toDefault() {
		count = 0;
		tot = 0;
	}

	public void fetch() throws Exception {
		toDefault();
		int[] years = populateYears();
		switch(frequency) {
		case ANNUAL:
			fetchAnnually(years);
			break;
		case QUARTERLY:
			fetchQuarterly(years);
			break;
		case MONTHLY:
			fetchMontly(years);
			break;
		}
	}

	private void fetchMontly(int[] years) {
		for(int y:years) {
			for(int i = 1; i < 13; i++) {
				String url = String.format(urlFormat, y, i);
				try {
					fetchData(url);
				} catch (IOException e) {
					logger.error("error while downloading an annual data file");
					logger.error(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private void fetchQuarterly(int[] years) {
		for(int y:years) {
			for(int i = 1; i < 5; i++) {
				String url = String.format(urlFormat, y, i);
				try {
					fetchData(url);
				} catch (IOException e) {
					logger.error("error while downloading an annual data file");
					logger.error(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}
	}

}
