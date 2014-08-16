package com.mauriciotogneri.andwars.objects;

import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.HitBuilders.EventBuilder;
import com.google.android.gms.analytics.Tracker;
import com.mauriciotogneri.andwars.R;
import com.mauriciotogneri.andwars.objects.players.Player;
import com.mauriciotogneri.andwars.states.Initialization;
import com.mauriciotogneri.andwars.states.TurnManager;
import com.mauriciotogneri.andwars.ui.renders.GameRenderer;

public class Game
{
	private final Map map;
	private final List<Player> players;
	private GameRenderer gameRenderer;
	private TurnManager turnManager;
	private final Tracker tracker;
	
	private boolean started = false;
	private boolean finished = false;
	private boolean screenLocked = true;
	
	private final List<OnCellSelected> onCellSelectedListeners = new ArrayList<OnCellSelected>();
	
	public static final int NUMBER_INITIAL_CELLS = 5;
	public static final int NUMBER_MOVES_PER_PLAYER = 2;
	
	private enum GameAction
	{
		START, RESTART, WIN, LOSE, TIE, CLOSE;
	}
	
	public enum GameResult
	{
		WIN(R.string.dialog_game_finished_won), LOSE(R.string.dialog_game_finished_lost), TIE(R.string.dialog_game_finished_tie);
		
		private final int textId;
		
		private GameResult(int textId)
		{
			this.textId = textId;
		}
		
		public int getTextId()
		{
			return this.textId;
		}
	}
	
	public Game(Map map, List<Player> players, Tracker tracker)
	{
		this.map = map;
		this.players = players;
		
		for (Player player : players)
		{
			player.initialize(this);
		}
		
		this.tracker = tracker;
	}
	
	public void setGameRenderer(GameRenderer gameRenderer)
	{
		this.gameRenderer = gameRenderer;
		updateMap();
	}
	
	public void start()
	{
		sendHit(GameAction.START);
		
		this.started = true;
		this.finished = false;
		
		updateMap();
		
		Initialization initialization = new Initialization(this, this.players);
		initialization.start();
	}
	
	public void restart()
	{
		sendHit(GameAction.RESTART);
		
		this.finished = false;
		
		lockScreen();
		
		this.map.restart();
		updateMap();
		
		Initialization initialization = new Initialization(this, this.players);
		initialization.start();
	}
	
	public boolean isStarted()
	{
		return this.started;
	}
	
	private synchronized boolean isScreenLocked()
	{
		return this.screenLocked;
	}
	
	public synchronized void lockScreen()
	{
		this.screenLocked = true;
		this.gameRenderer.lockButtons();
	}
	
	public synchronized void unlockScreen(boolean unlockButtons)
	{
		this.screenLocked = false;
		
		if (unlockButtons)
		{
			this.gameRenderer.unlockButtons();
		}
	}
	
	public Map getMap()
	{
		return this.map;
	}
	
	public void updateMap()
	{
		this.gameRenderer.update(this.map);
	}
	
	public void updateMap(Move move)
	{
		this.gameRenderer.update(this.map, move);
	}
	
	public void updateTurnNumber(int turn)
	{
		this.gameRenderer.updateTurnNumber(turn);
	}
	
	public void onClick(int x, int y)
	{
		if (!isScreenLocked())
		{
			List<Cell> cells = this.map.getCells();
			
			for (Cell cell : cells)
			{
				if ((cell.x == x) && (cell.y == y))
				{
					for (OnCellSelected listener : this.onCellSelectedListeners)
					{
						listener.onCellSelected(cell);
					}
					break;
				}
			}
		}
	}
	
	public void addOnCellSelectedListener(OnCellSelected listener)
	{
		this.onCellSelectedListeners.add(listener);
	}
	
	public void removeOnCellSelectedListener(OnCellSelected listener)
	{
		this.onCellSelectedListeners.remove(listener);
	}
	
	public void gameInitialized()
	{
		this.turnManager = new TurnManager(this, this.players);
		this.turnManager.start();
	}
	
	public void updateUnits()
	{
		this.map.updateUnits(this);
		updateMap();
	}
	
	public boolean isFinished()
	{
		return this.finished;
	}
	
	public void gameFinished(Player winner)
	{
		this.finished = true;
		
		if (winner.isHuman())
		{
			sendHit(GameAction.WIN);
			this.gameRenderer.showEndMessage(GameResult.WIN);
		}
		else
		{
			sendHit(GameAction.LOSE);
			this.gameRenderer.showEndMessage(GameResult.LOSE);
		}
	}
	
	public void gameTie()
	{
		sendHit(GameAction.TIE);

		this.finished = true;
		
		this.gameRenderer.showEndMessage(GameResult.TIE);
	}
	
	public void passTurn()
	{
		if ((!isScreenLocked()) && (this.turnManager != null))
		{
			this.turnManager.passTurn();
		}
	}
	
	public void close()
	{
		sendHit(GameAction.CLOSE);
	}
	
	private void sendHit(final GameAction action)
	{
		final String mapName = this.map.toString();
		
		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				EventBuilder builder = new HitBuilders.EventBuilder();
				builder.setCategory(mapName);
				builder.setAction(action.toString());
				
				Game.this.tracker.send(builder.build());
			}
		});
		thread.start();
	}
	
	public interface OnCellSelected
	{
		void onCellSelected(Cell cell);
	}
}