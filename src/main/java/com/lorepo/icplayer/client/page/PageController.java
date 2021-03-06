package com.lorepo.icplayer.client.page;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.Window;
import com.lorepo.icf.scripting.ICommandReceiver;
import com.lorepo.icf.scripting.ScriptParserException;
import com.lorepo.icf.scripting.ScriptingEngine;
import com.lorepo.icplayer.client.IPlayerController;
import com.lorepo.icplayer.client.content.services.PlayerServices;
import com.lorepo.icplayer.client.model.Page;
import com.lorepo.icplayer.client.model.Page.ScoringType;
import com.lorepo.icplayer.client.module.IModuleFactory;
import com.lorepo.icplayer.client.module.ModuleFactory;
import com.lorepo.icplayer.client.module.api.IModuleModel;
import com.lorepo.icplayer.client.module.api.IModuleView;
import com.lorepo.icplayer.client.module.api.IPresenter;
import com.lorepo.icplayer.client.module.api.IStateful;
import com.lorepo.icplayer.client.module.api.event.PageLoadedEvent;
import com.lorepo.icplayer.client.module.api.event.ResetPageEvent;
import com.lorepo.icplayer.client.module.api.event.ShowErrorsEvent;
import com.lorepo.icplayer.client.module.api.event.WorkModeEvent;
import com.lorepo.icplayer.client.module.api.player.IPage;
import com.lorepo.icplayer.client.module.api.player.IPlayerServices;
import com.lorepo.icplayer.client.module.api.player.PageScore;

public class PageController {

	public interface IPageDisplay{

		void addModuleView(IModuleView view, IModuleModel module);
		void setPage(Page page);
		void refreshMathJax();
		void setWidth(int width);
		void setHeight(int height);
		void removeAllModules();
	}
	
	private IPageDisplay pageView;
	private Page	currentPage;
	private PlayerServices playerServiceImpl;
	private IPlayerServices playerService;
	private IModuleFactory moduleFactory;
	private ArrayList<IPresenter>	presenters;
	private ScriptingEngine scriptingEngine = new ScriptingEngine();
	private IPlayerController playerController;
	
	
	public PageController(IPlayerController playerController) {
		this.playerController = playerController;
		playerServiceImpl = new PlayerServices(playerController, this);
		init(playerServiceImpl);
	}
	
	
	public PageController(IPlayerServices playerServices) {
		init(playerServices);
	}


	private void init(IPlayerServices playerServices) {
		presenters = new ArrayList<IPresenter>();
		this.playerService = playerServices;
		moduleFactory = new ModuleFactory(playerService);
	}


	public void setView(IPageDisplay view){
		pageView = view;
	}

	
	protected void setModuleFactory(IModuleFactory factory) {
		this.moduleFactory = factory;
	}

	
	public void setPage(Page page){
		
		if(playerServiceImpl != null){
			playerServiceImpl.resetEventBus();
		}
		currentPage = page;
		pageView.setPage(page);
		setViewSize(page);
		initModules();
		if(playerService.getStateService() != null){
			HashMap<String, String> state = playerService.getStateService().getStates();
			setPageState(state);
		}
		pageView.refreshMathJax();
		playerService.getEventBus().fireEvent(new PageLoadedEvent(page.getName()));
	}


	private void setViewSize(Page page) {
		
		if(page.getWidth() > 0){
			pageView.setWidth(page.getWidth());
		}
		if(page.getHeight() > 0){
			pageView.setHeight(page.getHeight());
		}
	}


	private void initModules() {
		
		presenters.clear();
		pageView.removeAllModules();
		scriptingEngine.reset();
		
		for(IModuleModel module : currentPage.getModules()){

			IModuleView moduleView = moduleFactory.createView(module);
			IPresenter presenter = moduleFactory.createPresenter(module);
			pageView.addModuleView(moduleView, module);
			if(presenter != null){
				presenter.addView(moduleView);
				presenters.add(presenter);
				if(presenter instanceof ICommandReceiver){
					scriptingEngine.addReceiver((ICommandReceiver) presenter);
				}
			}
			else if(moduleView instanceof IPresenter){
				presenters.add((IPresenter) moduleView);
			}
		}
	}


	public void checkAnswers() {
		updateScore(true);
		playerService.getEventBus().fireEvent(new ShowErrorsEvent());
	}


	public void updateScore(boolean updateCounters) {
		if(currentPage != null){

			Score.Result result;
			if(currentPage.getScoringType() == ScoringType.zeroOne){
				result = Score.calculateZeroOneScore(presenters);
			}else if(currentPage.getScoringType() == ScoringType.minusErrors){
				result = Score.calculateMinusScore(presenters);
			}else{
				result = Score.calculatePercentageScore(presenters);
			}
	
			if(currentPage.isReportable()){
				PageScore pageScore = playerService.getScoreService().getPageScore(currentPage.getName());
				PageScore score = pageScore.updateScore(result.score, result.maxScore, result.errorCount);
				if(updateCounters){
					playerService.getScoreService().setPageScore(currentPage, score.incrementCounters());
				}
				else{
					playerService.getScoreService().setPageScore(currentPage, score);
				}
			}
		}
	}

	
	public void uncheckAnswers() {
		
		playerService.getEventBus().fireEvent(new WorkModeEvent());
	}


	public void reset() {
		
		if(currentPage.isReportable()){
			PageScore pageScore = playerService.getScoreService().getPageScore(currentPage.getName());
			if(pageScore.hasScore()){
				PageScore score = pageScore.reset();
				playerService.getScoreService().setPageScore(currentPage, score);
			}
		}
		playerService.getEventBus().fireEvent(new ResetPageEvent());
	}

	
	public PageScore getPageScore() {

		if(currentPage == null || !currentPage.isReportable()){
			return null;
		}
		
		return playerService.getScoreService().getPageScore(currentPage.getName());
	}


	public void setPageState(HashMap<String, String> state) {
		
		for(IPresenter presenter : presenters){
			if(presenter instanceof IStateful){
				IStateful statefulObj = (IStateful)presenter;
				String key = currentPage.getId() + statefulObj.getSerialId(); 
				String moduleState = state.get(key);
				if(moduleState != null){
					statefulObj.setState(moduleState);
				}				
			}
		}
	}


	public HashMap<String, String> getState() {
		
		HashMap<String, String>	pageState = new HashMap<String, String>();
		if(currentPage != null){
			for(IPresenter presenter : presenters){
				if(presenter instanceof IStateful){
					IStateful statefulObj = (IStateful)presenter;
					String state = statefulObj.getState();
					String key = currentPage.getId() + statefulObj.getSerialId();
					pageState.put(key, state);
				}
			}
		}
		return pageState;
	}


	public void runScript(String script){

		try {
			scriptingEngine.execute(script);
		} catch (ScriptParserException e) {
			Window.alert(e.getMessage());
		}
	}
	
	
	public IPresenter findModule(String name){
		
		for(IPresenter presenter : presenters){
		
			if(presenter.getModel().getId().compareTo(name) == 0){
				return presenter;
			}
		}
		
		return null;
	}


	public IPage getPage() {
		return currentPage;
	}


	public void closePage() {
		if(playerServiceImpl != null){
			playerServiceImpl.resetEventBus();
		}
		if(currentPage != null){ 
			currentPage.release();
			currentPage = null;
		}
		pageView.removeAllModules();
	}


	public IPlayerServices getPlayerServices() {
		return playerService;
	}
	
	public IPlayerController getPlayerController() {
		return playerController;
	}
}
