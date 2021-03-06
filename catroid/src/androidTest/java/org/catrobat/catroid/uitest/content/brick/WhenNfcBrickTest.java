/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2017 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.uitest.content.brick;

import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.BrickValues;
import org.catrobat.catroid.common.NfcTagData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.SingleSprite;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenNfcScript;
import org.catrobat.catroid.content.bricks.PlaySoundBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.WhenNfcBrick;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.nfc.NfcHandler;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.test.utils.Reflection;
import org.catrobat.catroid.ui.MainMenuActivity;
import org.catrobat.catroid.ui.ProgramMenuActivity;
import org.catrobat.catroid.ui.ScriptActivity;
import org.catrobat.catroid.ui.fragment.NfcTagFragment;
import org.catrobat.catroid.ui.fragment.ScriptFragment;
import org.catrobat.catroid.uitest.util.BaseActivityInstrumentationTestCase;
import org.catrobat.catroid.uitest.util.UiTestUtils;

import java.io.File;
import java.util.List;

public class WhenNfcBrickTest extends BaseActivityInstrumentationTestCase<MainMenuActivity> {

	private List<NfcTagData> tagDataList;

	private static final String FIRST_TEST_TAG_NAME = "tagNameTest";
	private static final String FIRST_TEST_TAG_ID = "111111";

	private static final String SECOND_TEST_TAG_NAME = "tagNameTest2";
	private static final String SECOND_TEST_TAG_ID = "222222";

	private String all;

	private static final int RESOURCE_SOUND = org.catrobat.catroid.test.R.raw.longsound;

	private String soundName = "testSound";
	private File soundFile;
	private List<SoundInfo> soundInfoList;

	public WhenNfcBrickTest() {
		super(MainMenuActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		UiTestUtils.enableNfcBricks(getActivity().getApplicationContext());
		createProject();
		all = solo.getString(R.string.brick_when_nfc_default_all);
		UiTestUtils.prepareStageForTest();
		UiTestUtils.getIntoScriptActivityFromMainMenu(solo);
	}

	public void testSelectTagAndPlay() {
		assertTrue(all + " is not selected in Spinner", solo.isSpinnerTextSelected(all));
		solo.clickOnText(all);
		solo.clickOnText(FIRST_TEST_TAG_NAME);
		assertTrue(FIRST_TEST_TAG_NAME + " is not selected in Spinner", solo.isSpinnerTextSelected(FIRST_TEST_TAG_NAME));

		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);

		solo.waitForActivity(StageActivity.class.getSimpleName());
		solo.sleep(1000);
		Script script = ProjectManager.getInstance().getCurrentScene().getSpriteList().get(0).getScript(0);
		assertEquals("tag not set", ((WhenNfcBrick) script.getScriptBrick()).getNfcTag().getNfcTagName(), tagDataList
				.get(0).getNfcTagName());
		solo.goBack();
		solo.goBack();
		solo.waitForActivity(ScriptActivity.class.getSimpleName());

		solo.clickOnText(FIRST_TEST_TAG_NAME);
		solo.clickOnText(SECOND_TEST_TAG_NAME);
		assertTrue(SECOND_TEST_TAG_NAME + " is not selected in Spinner", solo.searchText(SECOND_TEST_TAG_NAME));

		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);

		solo.waitForActivity(StageActivity.class.getSimpleName());
		solo.sleep(1000);
		script = ProjectManager.getInstance().getCurrentScene().getSpriteList().get(0).getScript(0);
		assertEquals("tag not set", ((WhenNfcBrick) script.getScriptBrick()).getNfcTag().getNfcTagName(), tagDataList
				.get(1).getNfcTagName());
	}

	public void testSpinnerUpdatesDelete() {
		solo.clickOnText(all);

		assertTrue(FIRST_TEST_TAG_NAME + " is not in Spinner", solo.searchText(FIRST_TEST_TAG_NAME));
		assertTrue(SECOND_TEST_TAG_NAME + " is not in Spinner", solo.searchText(SECOND_TEST_TAG_NAME));

		solo.goBack();
		solo.goBack();
		solo.clickOnText(solo.getString(R.string.nfctags));

		clickOnContextMenuItem(SECOND_TEST_TAG_NAME, solo.getString(R.string.delete), R.id.delete);

		UiTestUtils.switchToFragmentInScriptActivity(solo, UiTestUtils.SCRIPTS_INDEX);

		solo.clickOnText(all);

		assertFalse(SECOND_TEST_TAG_NAME + " is still in Spinner", solo.searchText(SECOND_TEST_TAG_NAME));
		assertTrue(FIRST_TEST_TAG_NAME + " is not in Spinner", solo.searchText(FIRST_TEST_TAG_NAME));
	}

	public void testSpinnerUpdatesRename() {
		String newName = "nameRenamed";

		solo.clickOnText(all);

		assertTrue(FIRST_TEST_TAG_NAME + " is not in Spinner", solo.searchText(FIRST_TEST_TAG_NAME));
		assertTrue(SECOND_TEST_TAG_NAME + " is not in Spinner", solo.searchText(SECOND_TEST_TAG_NAME));

		solo.clickOnText(FIRST_TEST_TAG_NAME);

		solo.goBack();

		solo.clickOnText(solo.getString(R.string.nfctags));

		clickOnContextMenuItem(FIRST_TEST_TAG_NAME, solo.getString(R.string.rename), R.id.rename);
		solo.clearEditText(0);
		solo.enterText(0, newName);
		solo.clickOnButton(solo.getString(R.string.ok));
		UiTestUtils.switchToFragmentInScriptActivity(solo, UiTestUtils.SCRIPTS_INDEX);

		solo.clickOnText(newName);

		assertTrue(newName + " is not in Spinner", solo.searchText(newName));
		assertTrue(SECOND_TEST_TAG_NAME + " is not in Spinner", solo.searchText(SECOND_TEST_TAG_NAME));
	}

	public void testAdapterUpdateInScriptActivity() {
		assertTrue(all + " is not selected in Spinner", solo.isSpinnerTextSelected(all));
		solo.clickOnText(all);
		solo.clickOnText(FIRST_TEST_TAG_NAME);
		assertTrue(FIRST_TEST_TAG_NAME + " is not selected in Spinner", solo.isSpinnerTextSelected(FIRST_TEST_TAG_NAME));

		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);
		solo.waitForActivity(StageActivity.class.getSimpleName());
		String tagName = ProjectManager.getInstance().getCurrentSprite().getNfcTagList().get(0).getNfcTagName();
		assertEquals("Wrong tag name set in stage", tagName, tagDataList.get(0).getNfcTagName());
		solo.sleep(500);
		solo.goBack();
		solo.sleep(100);
		solo.goBack();

		for (int i = 0; i < 5; ++i) {
			selectTag(SECOND_TEST_TAG_NAME, FIRST_TEST_TAG_NAME);
			selectTag(FIRST_TEST_TAG_NAME, SECOND_TEST_TAG_NAME);
		}
	}

	public void testPlayTriggerAll() {
		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);
		solo.waitForActivity(StageActivity.class.getSimpleName());
		solo.sleep(2000);

		WhenNfcScript script = (WhenNfcScript) ProjectManager.getInstance().getCurrentSprite().getScript(0);
		assertEquals("Wrong tag used in stage --> Problem with Adapter update in Script", script.isMatchAll(), true);

		UiTestUtils.fakeNfcTag(solo, "123456", null, null);

		solo.sleep(2000);
		MediaPlayer mediaPlayer = getMediaPlayers().get(0);
		assertTrue("mediaPlayer is not playing", mediaPlayer.isPlaying());
		assertEquals("wrong file playing", 7592, mediaPlayer.getDuration());
	}

	public void testPlayTriggerOne() {
		solo.clickOnText(all);
		solo.sleep(500);
		solo.clickOnText(FIRST_TEST_TAG_NAME);
		solo.sleep(500);
		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);
		solo.waitForActivity(StageActivity.class.getSimpleName());
		solo.sleep(2000);

		WhenNfcScript script = (WhenNfcScript) ProjectManager.getInstance().getCurrentSprite().getScript(0);
		assertEquals("Wrong tag used in stage --> Problem with Adapter update in Script", script.isMatchAll(), false);
		String tagName = ProjectManager.getInstance().getCurrentSprite().getNfcTagList().get(0).getNfcTagName();
		assertEquals("Wrong tag name set in stage", tagName, tagDataList.get(0).getNfcTagName());
		assertEquals("Wrong tag name set in stage", tagName, FIRST_TEST_TAG_NAME);

		UiTestUtils.fakeNfcTag(solo, SECOND_TEST_TAG_ID, null, null);
		solo.sleep(2000);
		MediaPlayer mediaPlayer;
		if (getMediaPlayers().size() > 0) {
			mediaPlayer = getMediaPlayers().get(0);
			assertFalse("mediaPlayer is playing", mediaPlayer.isPlaying());
		}
		solo.sleep(1000);
		UiTestUtils.fakeNfcTag(solo, FIRST_TEST_TAG_ID, null, null);
		solo.sleep(2000);
		mediaPlayer = getMediaPlayers().get(0);
		assertTrue("mediaPlayer is not playing", mediaPlayer.isPlaying());
		assertEquals("wrong file playing", 7592, mediaPlayer.getDuration());
	}

	public void testAddNewTag() {
		String newText = solo.getString(R.string.new_nfc_tag);

		solo.clickOnText(all);
		solo.clickOnText(newText);

		solo.waitForFragmentByTag(NfcTagFragment.TAG);
		solo.sleep(500);

		UiTestUtils.fakeNfcTag(solo, "123456", null, null);

		solo.sleep(500);

		solo.goBack();
		solo.waitForFragmentByTag(ScriptFragment.TAG);
		solo.clickOnText(all);
		assertTrue("Testtag not added", solo.searchText(solo.getString(R.string.default_tag_name)));
		solo.clickOnText(solo.getString(R.string.default_tag_name));

		assertTrue(solo.getString(R.string.default_tag_name) + " is not selected in Spinner", solo.isSpinnerTextSelected(solo.getString(R.string.default_tag_name)));

		solo.goBack();
		String programMenuActivityClass = ProgramMenuActivity.class.getSimpleName();
		assertTrue("Should be in " + programMenuActivityClass, solo.getCurrentActivity().getClass().getSimpleName()
				.equals(programMenuActivityClass));
	}

	public void testNfcSensorVariable() throws InterpretationException {
		String nfcIdString1 = "f0afb8b4";
		String nfcIdString2 = "10caffee";
		String nfcNdefString1 = "I am the first!";
		String nfcNdefString2 = "And I am the second!";
		NdefMessage ndefMessage1 = NfcHandler.createMessage(nfcNdefString1, BrickValues.TNF_MIME_MEDIA);
		NdefMessage ndefMessage2 = NfcHandler.createMessage(nfcNdefString2, BrickValues.TNF_MIME_MEDIA);
		String tagId1 = NfcHandler.byteArrayToHex(nfcIdString1.getBytes());
		String tagId2 = NfcHandler.byteArrayToHex(nfcIdString2.getBytes());
		int waitingTime = 500;
		String newText = solo.getString(R.string.new_nfc_tag);

		solo.clickOnText(all);
		solo.clickOnText(newText);  // Error API 23.: Clicks on variable TagNameTest2
		solo.waitForFragmentByTag(NfcTagFragment.TAG);
		solo.sleep(waitingTime);

		UiTestUtils.fakeNfcTag(solo, nfcIdString1, ndefMessage1, null);
		solo.sleep(waitingTime);
		solo.goBack();
		solo.waitForView(solo.getView(R.id.brick_set_variable_edit_text));

		checkSensorValue(solo.getString(R.string.formula_editor_nfc_tag_id), tagId1);
		checkSensorValue(solo.getString(R.string.formula_editor_nfc_tag_message), nfcNdefString1);

		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);
		solo.waitForActivity(StageActivity.class.getSimpleName());

		UiTestUtils.fakeNfcTag(solo, nfcIdString2, ndefMessage2, null);
		solo.sleep(waitingTime);
		solo.goBack();
		solo.sleep(waitingTime);
		solo.clickOnButton(solo.getString(R.string.stage_dialog_back));
		solo.waitForView(solo.getView(R.id.brick_set_variable_edit_text));

		checkSensorValue(solo.getString(R.string.formula_editor_nfc_tag_id), tagId2);
		checkSensorValue(solo.getString(R.string.formula_editor_nfc_tag_message), nfcNdefString2);
	}

	private void checkSensorValue(String sensorName, String expectedValue) {
		solo.clickOnText(sensorName);
		solo.clickOnView(solo.getView(R.id.formula_editor_keyboard_compute));
		solo.waitForView(solo.getView(R.id.formula_editor_compute_dialog_textview));
		TextView computeTextView = (TextView) solo.getView(R.id.formula_editor_compute_dialog_textview);
		String sensorValue = computeTextView.getText().toString();

		assertEquals(sensorValue, expectedValue);
		solo.goBack();
		solo.waitForView(solo.getView(R.id.formula_editor_edit_field));
		solo.goBack();
	}

	private void selectTag(String newTag, String oldName) {
		solo.clickOnText(oldName);
		solo.clickOnText(newTag);
		UiTestUtils.clickOnBottomBar(solo, R.id.button_play);
		solo.sleep(5000);
		solo.waitForActivity(StageActivity.class.getSimpleName());
		solo.sleep(2000);
		String tagName = ((WhenNfcBrick) ProjectManager.getInstance().getCurrentSprite().getScript(0).getScriptBrick()).getNfcTag().getNfcTagName();
		assertEquals("Wrong tag used in stage --> Problem with Adapter update in Script", newTag, tagName);
		solo.goBack();
		solo.goBack();
	}

	private void createProject() {
		ProjectManager projectManager = ProjectManager.getInstance();
		Project project = new Project(null, UiTestUtils.DEFAULT_TEST_PROJECT_NAME);
		Sprite firstSprite = new SingleSprite("cat");
		Script testScript = new WhenNfcScript();

		PlaySoundBrick playSoundBrick = new PlaySoundBrick();
		testScript.addBrick(playSoundBrick);

		SetVariableBrick setVariableBrickId = new SetVariableBrick(Sensors.NFC_TAG_ID);
		testScript.addBrick(setVariableBrickId);
		SetVariableBrick setVariableBrickMessage = new SetVariableBrick(Sensors.NFC_TAG_MESSAGE);
		testScript.addBrick(setVariableBrickMessage);

		firstSprite.addScript(testScript);
		project.getDefaultScene().addSprite(firstSprite);

		projectManager.setProject(project);
		projectManager.setCurrentSprite(firstSprite);
		projectManager.setCurrentScript(testScript);
		tagDataList = projectManager.getCurrentSprite().getNfcTagList();

		NfcTagData tagData = new NfcTagData();
		tagData.setNfcTagName(FIRST_TEST_TAG_NAME);
		tagData.setNfcTagUid(NfcHandler.byteArrayToHex(FIRST_TEST_TAG_ID.getBytes()));
		tagDataList.add(tagData);

		NfcTagData tagData2 = new NfcTagData();
		tagData2.setNfcTagName(SECOND_TEST_TAG_NAME);
		tagData2.setNfcTagUid(NfcHandler.byteArrayToHex(SECOND_TEST_TAG_ID.getBytes()));
		tagDataList.add(tagData2);

		soundInfoList = projectManager.getCurrentSprite().getSoundList();

		soundFile = UiTestUtils.saveFileToProject(UiTestUtils.DEFAULT_TEST_PROJECT_NAME, project.getDefaultScene().getName(), "longsound.mp3",
				RESOURCE_SOUND, getInstrumentation().getContext(), UiTestUtils.FileTypes.SOUND);
		SoundInfo soundInfo = new SoundInfo();
		soundInfo.setSoundFileName(soundFile.getName());
		soundInfo.setTitle(soundName);

		soundInfoList.add(soundInfo);
		ProjectManager.getInstance().getFileChecksumContainer()
				.addChecksum(soundInfo.getChecksum(), soundInfo.getAbsolutePath());
	}

	private void clickOnContextMenuItem(String tagName, String menuItemName, int id) {
		UiTestUtils.openActionMode(solo, menuItemName, id);
		solo.clickOnText(tagName);
		UiTestUtils.acceptAndCloseActionMode(solo);
		solo.waitForDialogToOpen();
		if (solo.waitForText(solo.getString(R.string.yes), 1, 800)) {
			solo.clickOnButton(solo.getString(R.string.yes));
			solo.waitForDialogToClose();
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaPlayer> getMediaPlayers() {
		return (List<MediaPlayer>) Reflection.getPrivateField(SoundManager.getInstance(), "mediaPlayers");
	}
}
