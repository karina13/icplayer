package com.lorepo.icplayer.client.module.choice;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.lorepo.icplayer.client.framework.module.StyleUtils;
import com.lorepo.icplayer.client.module.choice.ChoicePresenter.IOptionDisplay;
import com.lorepo.icplayer.client.utils.MathJax;

public class ChoiceView extends AbsolutePanel implements ChoicePresenter.IDisplay, ValueChangeHandler<Boolean>{

	private ChoiceModel module;
	private VerticalPanel optionsPanel;
	private ArrayList<IOptionDisplay>	optionWidgets = new ArrayList<IOptionDisplay>();
	private IOptionListener listener;
	
	
	public ChoiceView(ChoiceModel module, boolean isPreview){
	
		this.module = module;
		createUI(isPreview);
	}

	
	/**
	 * To zamieszanie z tworzeniem VerticalPanel jest potrzebne ponieważ bez tego 
	 * GWT głupieje.
	 * @param isPreview 
	 */
	private void createUI(boolean isPreview){
		
		optionsPanel = new VerticalPanel();

		optionsPanel.setStyleName("ic_choice");
		
		for(ChoiceOption option : module.getOptions()){
			OptionView widget;
			widget = new OptionView(option, module.isMulti());
			widget.addValueChangeHandler(this);
			optionWidgets.add(widget);
			optionsPanel.add((Widget)widget);
		}

		optionsPanel.setSize("100%", "100%");
		add(optionsPanel);
		setWidgetPosition(optionsPanel, 0, 0);
		
		StyleUtils.applyInlineStyle(this, module);
		if(!isPreview){
			setVisible(module.isVisible());
		}
		getElement().setId(module.getId());
		if(module.isDisabled()){
			setEnabled(false);
		}
	}
	    
	@Override
	public List<IOptionDisplay> getOptions() {
		return optionWidgets;
	}

	@Override
	public void addListener(IOptionListener listener) {
		this.listener = listener;
	}

	@Override
	public void onValueChange(ValueChangeEvent<Boolean> event) {

		if(listener != null){
			listener.onValueChange((IOptionDisplay) event.getSource(), event.getValue());
		}
		
	}

	@Override
	public void setEnabled(boolean b) {

		for(IOptionDisplay optionView : optionWidgets){
			OptionView widget = (OptionView) optionView;
			widget.setEnabled(b);
		}
	}

	@Override
	public void hide() {
		setVisible(false);
	}

	@Override
	public void show() {
		setVisible(true);
		refreshMath();
	}

	private void refreshMath() {
		MathJax.refreshMathJax(getElement());
	}
}
