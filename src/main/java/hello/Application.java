package hello;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;


import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
@RestController
public class Application {

  static class Self {
    public String href;
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  Random rand = new Random();
  int i = 0;
  int prevScore = 0;

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    writeCommittedStream.send(arenaUpdate.arena);


    PlayerState self = arenaUpdate.arena.state.get(arenaUpdate._links.self.href);

    if (self.wasHit) {
        if (rand.nextInt(5) == 0) {
            return "R";
        }
        return "F";
    }

    if (scoreNotChanged(self.score)) {
        if (rand.nextInt(3) == 0) {
            return "R";
        }
        return "F";
    }

    if (self.direction.equals("N")) {
      if (doIShootN(arenaUpdate, self)) return "T";
      return "R";
    } else if (self.direction.equals("E")) {
      if (doIShootE(arenaUpdate, self)) return "T";
      return "R";
    } else if (self.direction.equals("S")) {
      if (doIShootS(arenaUpdate, self)) return "T";
      return "R";
    } else {
      if (doIShootW(arenaUpdate, self)) return "T";
      if (rand.nextInt(5) == 0) {
        return "R";
        }
        return "F";
    }
  }

    private boolean scoreNotChanged(Integer score) {
        i++;
        if (i > 3) {
            i = 0;
            if (score <= prevScore) {
                prevScore = score;
                return true;
            } else {
                prevScore = score;
            }
        }
        return false;
    }

    private boolean doIShootN(ArenaUpdate arenaUpdate, PlayerState self) {
    return arenaUpdate.arena.state.entrySet()
            .stream()
            .filter(entry -> entry.getValue().x.equals(self.x))
            .filter(entry -> self.y - entry.getValue().y > 0)
            .anyMatch(entry -> self.y  - entry.getValue().y < 4);
  }

  private boolean doIShootE(ArenaUpdate arenaUpdate, PlayerState self) {
    return arenaUpdate.arena.state.entrySet()
            .stream()
            .filter(entry -> entry.getValue().x - self.x < 4)
            .filter(entry -> entry.getValue().x - self.x > 0)
            .anyMatch(entry -> entry.getValue().y == self.y);
  }

  private boolean doIShootS(ArenaUpdate arenaUpdate, PlayerState self) {
    return arenaUpdate.arena.state.entrySet()
            .stream()
            .filter(entry -> entry.getValue().x.equals(self.x))
            .filter(entry -> (entry.getValue().y) - (self.y) > 0)
            .anyMatch(entry -> (entry.getValue().y) - (self.y) < 4);
  }

  private boolean doIShootW(ArenaUpdate arenaUpdate, PlayerState self) {
    return arenaUpdate.arena.state.entrySet()
            .stream()
            .filter(entry -> self.x - entry.getValue().x < 4)
            .filter(entry -> self.x - entry.getValue().x > 0)
            .anyMatch(entry -> entry.getValue().y == self.y);
  }


    static class WriteCommittedStream {

        final JsonStreamWriter jsonStreamWriter;

        public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {

            try (BigQueryWriteClient client = BigQueryWriteClient.create()) {

                WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
                TableName parentTable = TableName.of(projectId, datasetName, tableName);
                CreateWriteStreamRequest createWriteStreamRequest =
                        CreateWriteStreamRequest.newBuilder()
                                .setParent(parentTable.toString())
                                .setWriteStream(stream)
                                .build();

                WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);

                jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
            }
        }

        public ApiFuture<AppendRowsResponse> send(Arena arena) {
            Instant now = Instant.now();
            JSONArray jsonArray = new JSONArray();

            arena.state.forEach((url, playerState) -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", playerState.x);
                jsonObject.put("y", playerState.y);
                jsonObject.put("direction", playerState.direction);
                jsonObject.put("wasHit", playerState.wasHit);
                jsonObject.put("score", playerState.score);
                jsonObject.put("player", url);
                jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
                jsonArray.put(jsonObject);
            });

            return jsonStreamWriter.append(jsonArray);
        }

    }

    final String projectId = ServiceOptions.getDefaultProjectId();
    final String datasetName = "snowball";
    final String tableName = "events";

    final WriteCommittedStream writeCommittedStream;

    public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
        writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
    }


}

