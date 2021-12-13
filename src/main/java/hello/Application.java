package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

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
    PlayerState self = arenaUpdate.arena.state.get(arenaUpdate._links.self.href);

    if (self.wasHit) {
        return goSomewhere();
    }

    if (self.direction.equals("N")) {
      if (doIShootN(arenaUpdate, self)) return "T";
    } else if (self.direction.equals("E")) {
      if (doIShootE(arenaUpdate, self)) return "T";
    } else if (self.direction.equals("S")) {
      if (doIShootS(arenaUpdate, self)) return "T";
    } else {
      if (doIShootW(arenaUpdate, self)) return "T";
    }

    return goSomewhere();
  }

    private String goSomewhere() {
      int whereTogo = rand.nextInt(5);
      if (whereTogo < 2) {
          return "F";
      } else if (whereTogo == 3) {
          return "R";
      } else {
          return "L";
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
            .filter(entry -> entry.getValue().y - self.y > 0)
            .anyMatch(entry -> entry.getValue().y - self.y < 4);
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
            .filter(entry -> (self.y) - (entry.getValue().y) > 0)
            .anyMatch(entry -> (self.y) - (entry.getValue().y) < 4);
  }

  private boolean doIShootW(ArenaUpdate arenaUpdate, PlayerState self) {
    return arenaUpdate.arena.state.entrySet()
            .stream()
            .filter(entry -> self.x - entry.getValue().x < 4)
            .filter(entry -> self.x - entry.getValue().x > 0)
            .anyMatch(entry -> entry.getValue().y == self.y);
  }

}

