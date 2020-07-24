//package eu.siacs.conversations.ui.util;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.opengl.EGLContext;
//import android.os.Build;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import com.google.common.base.Optional;
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.ImmutableSet;
//import com.google.common.util.concurrent.Futures;
//import com.google.common.util.concurrent.ListenableFuture;
//import com.google.common.util.concurrent.SettableFuture;
//
//import org.webrtc.CameraVideoCapturer;
//import org.webrtc.DataChannel;
//import org.webrtc.DefaultVideoDecoderFactory;
//import org.webrtc.DefaultVideoEncoderFactory;
//import org.webrtc.EglBase;
//import org.webrtc.IceCandidate;
//import org.webrtc.Logging;
//import org.webrtc.MediaCodecVideoEncoder;
//import org.webrtc.MediaConstraints;
//import org.webrtc.MediaStream;
//import org.webrtc.PeerConnection;
//import org.webrtc.PeerConnectionFactory;
//import org.webrtc.RtpReceiver;
//import org.webrtc.SdpObserver;
//import org.webrtc.SessionDescription;
//import org.webrtc.StatsObserver;
//import org.webrtc.StatsReport;
//import org.webrtc.SurfaceTextureHelper;
//import org.webrtc.SurfaceViewRenderer;
//import org.webrtc.VideoCapturer;
//import org.webrtc.VideoSource;
//import org.webrtc.VideoTrack;
//import org.webrtc.audio.JavaAudioDeviceModule;
//import org.webrtc.voiceengine.WebRtcAudioEffects;
//
//import java.util.EnumSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import eu.siacs.conversations.Config;
//import eu.siacs.conversations.services.AppRTCAudioManager;
//import eu.siacs.conversations.utils.IP;
//import eu.siacs.conversations.xmpp.jingle.AbstractJingleConnection;
//import eu.siacs.conversations.xmpp.jingle.JingleRtpConnection;
//import eu.siacs.conversations.xmpp.jingle.Media;
//import eu.siacs.conversations.xmpp.jingle.WebRTCWrapper;
//
//class PeerConnectionClient extends WebRTCWrapper {
//	public static final String VIDEO_TRACK_ID = "ARDAMSv0";
//	public static final String AUDIO_TRACK_ID = "ARDAMSa0";
//	private static final String TAG = "PCRTCClient";
//	private static final String FIELD_TRIAL_VP9 = "WebRTC-SupportVP9/Enabled/";
//	private static final String VIDEO_CODEC_VP8 = "VP8";
//	private static final String VIDEO_CODEC_VP9 = "VP9";
//	private static final String VIDEO_CODEC_H264 = "H264";
//	private static final String AUDIO_CODEC_OPUS = "opus";
//	private static final String AUDIO_CODEC_ISAC = "ISAC";
//	private static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
//	private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
//	private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
//	private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
//	private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
//	private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
//	private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
//	private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
//	private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
//	private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
//	private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
//	private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
//	private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
//	private static final int HD_VIDEO_WIDTH = 1280;
//	private static final int HD_VIDEO_HEIGHT = 720;
//	private static final int MAX_VIDEO_WIDTH = 1280;
//	private static final int MAX_VIDEO_HEIGHT = 1280;
//	private static final int MAX_VIDEO_FPS = 30;
//
//	private static final PeerConnectionClient instance = new PeerConnectionClient();
//	private static final int MAX_CONNECTIONS = 3;
//	// private final PCObserver pcObserver = new PCObserver();
//	private final PCObserver[] pcObservers = new PCObserver[MAX_CONNECTIONS];
//	// private final SDPObserver sdpObserver = new SDPObserver();
//	private final SDPObserver[] sdpObservers = new SDPObserver[MAX_CONNECTIONS];
//	PeerConnectionFactory.Options options = null;
//	private PeerConnectionFactory factory;
//	private PeerConnection[] peerConnections = new PeerConnection[MAX_CONNECTIONS];
//	private VideoSource videoSource;
//	private boolean videoCallEnabled;
//	private boolean audioCallEnabled;
//	private boolean preferIsac;
//	private boolean preferH264;
//	private boolean videoSourceStopped;
//	private boolean isError;
//	private Timer statsTimer;
//	private SurfaceViewRenderer localRender;
//	private SurfaceViewRenderer[] remoteRenders;
//	private MediaConstraints pcConstraints;
//	private MediaConstraints videoConstraints;
//	private MediaConstraints audioConstraints;
//	private MediaConstraints sdpMediaConstraints;
//	private PeerConnectionParameters peerConnectionParameters;
//	// Queued remote ICE candidates are consumed only after both local and
//	// remote descriptions are set. Similarly local ICE candidates are sent to
//	// remote peer after both local and remote description are set.
//	private LinkedList<IceCandidate>[] queuedRemoteCandidateLists = new LinkedList[MAX_CONNECTIONS];
//	private PeerConnectionEvents events;
//	private boolean[] isConnectionInitiator = new boolean[MAX_CONNECTIONS];
//	private SessionDescription[] localSdps = new SessionDescription[MAX_CONNECTIONS];
//			// either offer or answer SDP
//	private MediaStream mediaStream;
//	private int numberOfCameras;
//	// enableVideo is set to true if video should be rendered and sent.
//	private boolean renderVideo;
//	private VideoTrack localVideoTrack;
//	private VideoTrack[] remoteVideoTracks = new VideoTrack[MAX_CONNECTIONS];
//	private  EventCallback eventCallback;
//
//	private PeerConnectionClient() {
//		super(new EventCallback() {
//			@Override public void onIceCandidate(IceCandidate iceCandidate) {
//
//			}
//
//			@Override public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
//
//			}
//
//			@Override public void onAudioDeviceChanged(AppRTCAudioManager.AudioDevice selectedAudioDevice,
//			                                           Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
//
//			}
//		});
//		// Looper thread is started once in private ctor and is used for all
//		// peer connection API calls to ensure new peer connection factory is
//		// created on the same thread as previously destroyed factory.
//
//		pcObservers[0] = new PCObserver();
//		pcObservers[0].connectionId = 0;
//		pcObservers[1] = new PCObserver();
//		pcObservers[1].connectionId = 1;
//		pcObservers[2] = new PCObserver();
//		pcObservers[2].connectionId = 2;
//
//		sdpObservers[0] = new SDPObserver();
//		sdpObservers[0].connectionId = 0;
//		sdpObservers[1] = new SDPObserver();
//		sdpObservers[1].connectionId = 1;
//		sdpObservers[2] = new SDPObserver();
//		sdpObservers[2].connectionId = 2;
//	}
//
//	public static PeerConnectionClient getInstance() {
//		return instance;
//	}
//
//	private static String setStartBitrate(String codec, boolean isVideoCodec,
//	                                      String sdpDescription, int bitrateKbps) {
//		String[] lines = sdpDescription.split("\r\n");
//		int rtpmapLineIndex = -1;
//		boolean sdpFormatUpdated = false;
//		String codecRtpMap = null;
//		// Search for codec rtpmap in format
//		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding
//		// parameters>]
//		String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
//		Pattern codecPattern = Pattern.compile(regex);
//		for (int i = 0; i < lines.length; i++) {
//			Matcher codecMatcher = codecPattern.matcher(lines[i]);
//			if (codecMatcher.matches()) {
//				codecRtpMap = codecMatcher.group(1);
//				rtpmapLineIndex = i;
//				break;
//			}
//		}
//		if (codecRtpMap == null) {
//			Log.w(TAG, "No rtpmap for " + codec + " codec");
//			return sdpDescription;
//		}
//		Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at "
//				+ lines[rtpmapLineIndex]);
//
//		// Check if a=fmtp string already exist in remote SDP for this codec and
//		// update it with new bitrate parameter.
//		regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
//		codecPattern = Pattern.compile(regex);
//		for (int i = 0; i < lines.length; i++) {
//			Matcher codecMatcher = codecPattern.matcher(lines[i]);
//			if (codecMatcher.matches()) {
//				Log.d(TAG, "Found " + codec + " " + lines[i]);
//				if (isVideoCodec) {
//					lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE + "="
//							+ bitrateKbps;
//				} else {
//					lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "="
//							+ (bitrateKbps * 1000);
//				}
//				Log.d(TAG, "Update remote SDP line: " + lines[i]);
//				sdpFormatUpdated = true;
//				break;
//			}
//		}
//
//		StringBuilder newSdpDescription = new StringBuilder();
//		for (int i = 0; i < lines.length; i++) {
//			newSdpDescription.append(lines[i]).append("\r\n");
//			// Append new a=fmtp line if no such line exist for a codec.
//			if (!sdpFormatUpdated && i == rtpmapLineIndex) {
//				String bitrateSet;
//				if (isVideoCodec) {
//					bitrateSet = "a=fmtp:" + codecRtpMap + " "
//							+ VIDEO_CODEC_PARAM_START_BITRATE + "="
//							+ bitrateKbps;
//				} else {
//					bitrateSet = "a=fmtp:" + codecRtpMap + " "
//							+ AUDIO_CODEC_PARAM_BITRATE + "="
//							+ (bitrateKbps * 1000);
//				}
//				Log.d(TAG, "Add remote SDP line: " + bitrateSet);
//				newSdpDescription.append(bitrateSet).append("\r\n");
//			}
//
//		}
//		return newSdpDescription.toString();
//	}
//
//	private static String preferCodec(String sdpDescription, String codec,
//	                                  boolean isAudio) {
//		String[] lines = sdpDescription.split("\r\n");
//		int mLineIndex = -1;
//		String codecRtpMap = null;
//		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding
//		// parameters>]
//		String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
//		Pattern codecPattern = Pattern.compile(regex);
//		String mediaDescription = "m=video ";
//		if (isAudio) {
//			mediaDescription = "m=audio ";
//		}
//		for (int i = 0; (i < lines.length)
//				&& (mLineIndex == -1 || codecRtpMap == null); i++) {
//			if (lines[i].startsWith(mediaDescription)) {
//				mLineIndex = i;
//				continue;
//			}
//			Matcher codecMatcher = codecPattern.matcher(lines[i]);
//			if (codecMatcher.matches()) {
//				codecRtpMap = codecMatcher.group(1);
//				continue;
//			}
//		}
//		if (mLineIndex == -1) {
//			Log.w(TAG, "No " + mediaDescription + " line, so can't prefer "
//					+ codec);
//			return sdpDescription;
//		}
//		if (codecRtpMap == null) {
//			Log.w(TAG, "No rtpmap for " + codec);
//			return sdpDescription;
//		}
//		Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at "
//				+ lines[mLineIndex]);
//		String[] origMLineParts = lines[mLineIndex].split(" ");
//		if (origMLineParts.length > 3) {
//			StringBuilder newMLine = new StringBuilder();
//			int origPartIndex = 0;
//			// Format is: m=<media> <port> <proto> <fmt> ...
//			newMLine.append(origMLineParts[origPartIndex++]).append(" ");
//			newMLine.append(origMLineParts[origPartIndex++]).append(" ");
//			newMLine.append(origMLineParts[origPartIndex++]).append(" ");
//			newMLine.append(codecRtpMap);
//			for (; origPartIndex < origMLineParts.length; origPartIndex++) {
//				if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
//					newMLine.append(" ").append(origMLineParts[origPartIndex]);
//				}
//			}
//			lines[mLineIndex] = newMLine.toString();
//			Log.d(TAG, "Change media description: " + lines[mLineIndex]);
//		} else {
//			Log.e(TAG, "Wrong SDP media description format: "
//					+ lines[mLineIndex]);
//		}
//		StringBuilder newSdpDescription = new StringBuilder();
//		for (String line : lines) {
//			newSdpDescription.append(line).append("\r\n");
//		}
//		return newSdpDescription.toString();
//	}
//
//	public void setPeerConnectionFactoryOptions(
//			PeerConnectionFactory.Options options) {
//		this.options = options;
//	}
//
//	public void createPeerConnectionFactory(final Context context,
//	                                        final EGLContext renderEGLContext,
//	                                        final PeerConnectionParameters peerConnectionParameters,
//	                                        final PeerConnectionEvents events) {
//		this.peerConnectionParameters = peerConnectionParameters;
//		this.events = events;
//		videoCallEnabled = peerConnectionParameters.videoCallEnabled;
//		audioCallEnabled = peerConnectionParameters.audioCallEnabled;
//		// Reset variables to initial states.
//		factory = null;
//		peerConnections[0] = null;
//		peerConnections[1] = null;
//		peerConnections[2] = null;
//		preferIsac = false;
//		preferH264 = false;
//		videoSourceStopped = false;
//		isError = false;
//		queuedRemoteCandidateLists[0] = null;
//		queuedRemoteCandidateLists[1] = null;
//		queuedRemoteCandidateLists[2] = null;
//		localSdps[0] = null; // either offer or answer SDP
//		localSdps[1] = null;
//		localSdps[2] = null;
//		mediaStream = null;
//		renderVideo = true;
//		localVideoTrack = null;
//		remoteVideoTracks[0] = null;
//		remoteVideoTracks[1] = null;
//		remoteVideoTracks[2] = null;
//		statsTimer = new Timer();
//
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				createPeerConnectionFactoryInternal(context, renderEGLContext);
//			}
//		});
//	}
//
//	public void createPeerConnection(final SurfaceViewRenderer localRender,
//	                                 final SurfaceViewRenderer remoteRender) {
//		if (peerConnectionParameters == null) {
//			Log.e(TAG, "Creating peer connection without initializing factory.");
//			return;
//		}
//		this.localRender = localRender;
//		this.remoteRenders = new SurfaceViewRenderer[MAX_CONNECTIONS];
//		this.remoteRenders[0] = remoteRender;
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				createMediaConstraintsInternal();
//				createPeerConnectionInternal(0);
//			}
//		});
//	}
//
//	public void createMultiPeerConnection(final SurfaceViewRenderer localRender,
//	                                      final SurfaceViewRenderer[] remoteRenders) {
//		if (peerConnectionParameters == null) {
//			Log.e(TAG, "Creating peer connection without initializing factory.");
//			return;
//		}
//		this.localRender = localRender;
//		this.remoteRenders = new SurfaceViewRenderer[MAX_CONNECTIONS];
//		this.remoteRenders[0] = remoteRenders[0];
//		this.remoteRenders[1] = remoteRenders[1];
//		this.remoteRenders[2] = remoteRenders[2];
//
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				createMediaConstraintsInternal();
//				createPeerConnectionInternal(0);
//				createPeerConnectionInternal(1);
//				createPeerConnectionInternal(2);
//			}
//		});
//	}
//
//	public void startCameraPreview(final SurfaceViewRenderer localRender) {
//		if (peerConnectionParameters == null) {
//			Log.e(TAG, "Creating peer connection without initializing factory.");
//			return;
//		}
//		this.localRender = localRender;
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				createMediaConstraintsInternal();
//				createMediaStream();
//			}
//		});
//	}
//
//	public void close() {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				closeAllConnections();
//				closeInternal();
//
//				events.onAllPeerConnectionsClosed();
//			}
//		});
//		// executor.requestStop();
//	}
//
//	public void resetConnection(final int connectionId) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				resetConnectionInternal(connectionId);
//			}
//		});
//	}
//
//	public void closeConnection(final int connectionId) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				closeConnectionInternal(connectionId);
//				events.onPeerConnectionClosed(connectionId);
//			}
//		});
//	}
//
//	public boolean isVideoCallEnabled() {
//		return videoCallEnabled;
//	}
//	//we should probably keep this in sync with: https://github.com/signalapp/Signal-Android/blob/master/app/src/main/java/org/thoughtcrime/securesms/ApplicationContext.java#L296
//	private static final Set<String> HARDWARE_AEC_BLACKLIST = new ImmutableSet.Builder<String>()
//			.add("Pixel")
//			.add("Pixel XL")
//			.add("Moto G5")
//			.add("Moto G (5S) Plus")
//			.add("Moto G4")
//			.add("TA-1053")
//			.add("Mi A1")
//			.add("Mi A2")
//			.add("E5823") // Sony z5 compact
//			.add("Redmi Note 5")
//			.add("FP2") // Fairphone FP2
//			.add("MI 5")
//			.build();
//	private void createPeerConnectionFactoryInternal(Context context,
//	                                                 EGLContext renderEGLContext) {
//		Log.d(TAG, "Create peer connection factory with EGLContext "
//				+ renderEGLContext + ". Use video: "
//				+ peerConnectionParameters.videoCallEnabled);
//		isError = false;
//		// Check if VP9 is used by default.
//		if (videoCallEnabled && peerConnectionParameters.videoCodec != null
//				&& peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_VP9)) {
//			PeerConnectionFactory.initializeFieldTrials(FIELD_TRIAL_VP9);
//		} else {
//			PeerConnectionFactory.initializeFieldTrials(null);
//		}
//		// Check if H.264 is used by default.
//		preferH264 = false;
//		if (videoCallEnabled && peerConnectionParameters.videoCodec != null
//				&& peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_H264)) {
//			preferH264 = true;
//		}
//		// Check if ISAC is used by default.
//		preferIsac = false;
//		if (peerConnectionParameters.audioCodec != null
//				&& peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC)) {
//			preferIsac = true;
//		}
//		PeerConnectionFactory.initialize(
//				PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
//		);
//		EglBase eglBase = EglBase.create();
//
//		final boolean setUseHardwareAcousticEchoCanceler = WebRtcAudioEffects.canUseAcousticEchoCanceler() && !HARDWARE_AEC_BLACKLIST.contains(
//				Build.MODEL);
//		Log.d(Config.LOGTAG, String.format("setUseHardwareAcousticEchoCanceler(%s) model=%s", setUseHardwareAcousticEchoCanceler, Build.MODEL));
//		factory = PeerConnectionFactory.builder()
//		                               .setVideoDecoderFactory(
//				                               new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
//		                               .setVideoEncoderFactory(
//				                               new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(),
//						                               true, true))
//		                               .setAudioDeviceModule(JavaAudioDeviceModule.builder(context)
//		                                                                          .setUseHardwareAcousticEchoCanceler(
//				                                                                          setUseHardwareAcousticEchoCanceler)
//		                                                                          .createAudioDeviceModule()
//		                               )
//		                               .createPeerConnectionFactory();
//	}
//
//	private void createMediaConstraintsInternal() {
//		if (pcConstraints != null)
//			return;
//		// Create peer connection constraints.
//		pcConstraints = new MediaConstraints();
//		// Enable DTLS for normal calls and disable for loopback calls.
//		if (peerConnectionParameters.loopback) {
//			pcConstraints.optional.add(new MediaConstraints.KeyValuePair(
//					DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false"));
//		} else {
//			pcConstraints.optional.add(new MediaConstraints.KeyValuePair(
//					DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
//		}
//
//		// Check if there is a camera on device and disable video call if not.
//		numberOfCameras = VideoCapturerAndroid.getDeviceCount();
//		if (numberOfCameras == 0) {
//			Log.w(TAG, "No camera on device. Switch to audio only call.");
//			videoCallEnabled = false;
//		}
//		// Create video constraints if video call is enabled.
//		if (videoCallEnabled) {
//			videoConstraints = new MediaConstraints();
//			int videoWidth = peerConnectionParameters.videoWidth;
//			int videoHeight = peerConnectionParameters.videoHeight;
//
//			// If VP8 HW video encoder is supported and video resolution is not
//			// specified force it to HD.
//			if ((videoWidth == 0 || videoHeight == 0)
//					&& peerConnectionParameters.videoCodecHwAcceleration
//					&& MediaCodecVideoEncoder.isVp8HwSupported()) {
//				videoWidth = HD_VIDEO_WIDTH;
//				videoHeight = HD_VIDEO_HEIGHT;
//			}
//
//			// Add video resolution constraints.
//			if (videoWidth > 0 && videoHeight > 0) {
//				videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
//				videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
//				videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//						MIN_VIDEO_WIDTH_CONSTRAINT, Integer
//						.toString(videoWidth)));
//				videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//						MAX_VIDEO_WIDTH_CONSTRAINT, Integer
//						.toString(videoWidth)));
//				videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//						MIN_VIDEO_HEIGHT_CONSTRAINT, Integer
//						.toString(videoHeight)));
//				videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//						MAX_VIDEO_HEIGHT_CONSTRAINT, Integer
//						.toString(videoHeight)));
//			}
//
//			// Add fps constraints.
//			int videoFps = peerConnectionParameters.videoFps;
//			if (videoFps > 0) {
//				videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
//				videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//						MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
//				videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//						MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
//			}
//		}
//
//		// Create audio constraints.
//		audioConstraints = new MediaConstraints();
//		// added for audio performance measurements
//		if (peerConnectionParameters.noAudioProcessing) {
//			Log.d(TAG, "Disabling audio processing");
//			audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//					AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
//			audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//					AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
//			audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//					AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
//			audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//					AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
//		}
//		// Create SDP constraints.
//		sdpMediaConstraints = new MediaConstraints();
//		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//				"OfferToReceiveAudio", "true"));
//		if (videoCallEnabled || peerConnectionParameters.loopback) {
//			sdpMediaConstraints.mandatory
//					.add(new MediaConstraints.KeyValuePair(
//							"OfferToReceiveVideo", "true"));
//		} else {
//			sdpMediaConstraints.mandatory
//					.add(new MediaConstraints.KeyValuePair(
//							"OfferToReceiveVideo", "false"));
//		}
//	}
//
//	private void createMediaStream() {
//		if (mediaStream == null) {
//			mediaStream = factory.createLocalMediaStream("ARDAMS");
//			if (videoCallEnabled) {
//				String cameraDeviceName = VideoCapturerAndroid.getDeviceName(0);
//				String frontCameraDeviceName = VideoCapturerAndroid
//						.getNameOfFrontFacingDevice();
//				if (numberOfCameras > 1 && frontCameraDeviceName != null) {
//					cameraDeviceName = frontCameraDeviceName;
//				}
//				Log.d(TAG, "Opening camera: " + cameraDeviceName);
//				videoCapturer = VideoCapturer.class(cameraDeviceName, null);
//
//				mediaStream.addTrack(createVideoTrack());
//			}
//
//			if (audioCallEnabled) {
//				mediaStream.addTrack(factory.createAudioTrack(AUDIO_TRACK_ID,
//						factory.createAudioSource(audioConstraints)));
//			}
//		}
//	}
//
//	private void createPeerConnectionInternal(int connectionId) {
//		if (factory == null || isError) {
//			Log.e(TAG, "Peerconnection factory is not created");
//			return;
//		}
//		Log.d(TAG, "Create peer connection");
//		Log.d(TAG, "PCConstraints: " + pcConstraints.toString());
//		if (videoConstraints != null) {
//			Log.d(TAG, "VideoConstraints: " + videoConstraints.toString());
//		}
//		queuedRemoteCandidateLists[connectionId] = new LinkedList<IceCandidate>();
//
//		PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(discoverIceServers());
//		// TCP candidates are only useful when connecting to a server that
//		// supports
//		// ICE-TCP.
//		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
//		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
//		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
//
//		peerConnections[connectionId] = factory.createPeerConnection(rtcConfig, pcConstraints,
//				pcObservers[connectionId]);
//		isConnectionInitiator[connectionId] = false;
//
//		// Set default WebRTC tracing and INFO libjingle logging.
//		// NOTE: this _must_ happen while |factory| is alive!
//		Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);
//
//		createMediaStream();
//
//		peerConnections[connectionId].addStream(mediaStream);
//
//		Log.d(TAG, "Peer connection created.");
//	}
//	private JingleRtpConnection requireRtpConnection() {
//
//		final JingleRtpConnection connection = this.rtpConnectionReference != null ? this.rtpConnectionReference.get() : null;
//		if (connection == null) {
//			throw new IllegalStateException("No RTP connection found");
//		}
//		return connection;
//	}	private void closeAllConnections() {
//		Log.d(TAG, "Closing all peer connections");
//
//		// close each of the peer connections, and call dispose on the last one:
//		int lastConnection = -1;
//		for (int i = 0; i < MAX_CONNECTIONS; i++) {
//			if (peerConnections[i] != null)
//				lastConnection = i;
//		}
//		for (int i = 0; i < MAX_CONNECTIONS; i++) {
//			if ((i != lastConnection) && (mediaStream != null) && (peerConnections[i] != null)) {
//				peerConnections[i].removeStream(mediaStream);
//				peerConnections[i].close();
//				peerConnections[i] = null;
//			}
//		}
//		// close the last connection and dispose of the stream:
//		if (lastConnection != -1) {
//			peerConnections[lastConnection].dispose();
//			peerConnections[lastConnection] = null;
//		} else {
//			if (mediaStream != null)
//				mediaStream.dispose();
//		}
//		Log.d(TAG, "Closing peer connection done.");
//	}
//
//	public boolean isPeerConnectionActive(int connectionId) {
//		return peerConnections[connectionId] != null;
//	}
//
//	public int getPeerConnectionCount() {
//		int connectionCount = 0;
//		for (int i = 0; i < MAX_CONNECTIONS; i++) {
//			if (peerConnections[i] != null)
//				connectionCount++;
//		}
//		return connectionCount;
//	}
//
//	private void resetConnectionInternal(int connectionId) {
//		closeConnectionInternal(connectionId);
//		localSdps[connectionId] = null;
//		createPeerConnectionInternal(connectionId);
//	}
//
//	private void closeConnectionInternal(int connectionId) {
//		Log.d(TAG, "Closing peer connection " + connectionId);
//
//		int connectionCount = getPeerConnectionCount();
//
//		// if this is the last connection then close and dispose, otherwise just remove the stream:
//		if (peerConnections[connectionId] != null) {
//			if (connectionCount == 1) {
//				peerConnections[connectionId].dispose();
//			} else {
//				peerConnections[connectionId].removeStream(mediaStream);
//				peerConnections[connectionId].close();
//			}
//			peerConnections[connectionId] = null;
//		}
//
//		events.onPeerConnectionClosed(connectionId);
//		Log.d(TAG, "Closing peer connection done.");
//	}
//
//	private void closeInternal() {
//		statsTimer.cancel();
//		Log.d(TAG, "Closing video source.");
//		if (videoSource != null) {
//			videoSource.dispose();
//			videoSource = null;
//		}
//		Log.d(TAG, "Closing peer connection factory.");
//		if (factory != null) {
//			factory.dispose();
//			factory = null;
//		}
//		options = null;
//	}
//
//	public boolean isHDVideo() {
//		if (!videoCallEnabled) {
//			return false;
//		}
//		int minWidth = 0;
//		int minHeight = 0;
//		for (MediaConstraints.KeyValuePair keyValuePair : videoConstraints.mandatory) {
//			if (keyValuePair.getKey().equals("minWidth")) {
//				try {
//					minWidth = Integer.parseInt(keyValuePair.getValue());
//				} catch (NumberFormatException e) {
//					Log.e(TAG,
//							"Can not parse video width from video constraints");
//				}
//			} else if (keyValuePair.getKey().equals("minHeight")) {
//				try {
//					minHeight = Integer.parseInt(keyValuePair.getValue());
//				} catch (NumberFormatException e) {
//					Log.e(TAG,
//							"Can not parse video height from video constraints");
//				}
//			}
//		}
//		if (minWidth * minHeight >= 1280 * 720) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	private void getStats(int connectionId) {
//		if (peerConnections[connectionId] == null || isError) {
//			return;
//		}
//		boolean success = peerConnections[connectionId].getStats(new StatsObserver() {
//			@Override
//			public void onComplete(final StatsReport[] reports) {
//				events.onPeerConnectionStatsReady(reports);
//			}
//		}, null);
//		if (!success) {
//			Log.e(TAG, "getStats() returns false!");
//		}
//	}
//
//	public void enableStatsEvents(boolean enable, int periodMs, final int connectionId) {
//		if (enable) {
//			try {
//				statsTimer.schedule(new TimerTask() {
//					@Override
//					public void run() {
//						new Handler().post(new Runnable() {
//							@Override
//							public void run() {
//								getStats(connectionId);
//							}
//						});
//					}
//				}, 0, periodMs);
//			} catch (Exception e) {
//				Log.e(TAG, "Can not schedule statistics timer " + e.toString());
//			}
//		} else {
//			statsTimer.cancel();
//		}
//	}
//
//	public void setVideoEnabled(final boolean enable) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				renderVideo = enable;
//				if (localVideoTrack != null) {
//					localVideoTrack.setEnabled(renderVideo);
//				}
//				if (remoteVideoTracks[0] != null) {
//					remoteVideoTracks[0].setEnabled(renderVideo);
//				}
//				if (remoteVideoTracks[1] != null) {
//					remoteVideoTracks[1].setEnabled(renderVideo);
//				}
//				if (remoteVideoTracks[2] != null) {
//					remoteVideoTracks[2].setEnabled(renderVideo);
//				}
//			}
//		});
//	}
//
//	public void createOffer(final int connectionId) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (peerConnections[connectionId] != null && !isError) {
//					Log.d(TAG, "PC Create OFFER");
//					isConnectionInitiator[connectionId] = true;
//					peerConnections[connectionId]
//							.createOffer(sdpObservers[connectionId], sdpMediaConstraints);
//				}
//			}
//		});
//	}
//
//	public void createAnswer(final int connectionId) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (peerConnections[connectionId] != null && !isError) {
//					Log.d(TAG, "PC create ANSWER");
//					isConnectionInitiator[connectionId] = false;
//					peerConnections[connectionId].createAnswer(sdpObservers[connectionId],
//							sdpMediaConstraints);
//				}
//			}
//		});
//	}
//
//	public void addRemoteIceCandidate(final IceCandidate candidate, final int connectionId) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (peerConnections[connectionId] != null && !isError) {
//					if (queuedRemoteCandidateLists[connectionId] != null) {
//						queuedRemoteCandidateLists[connectionId].add(candidate);
//					} else {
//						peerConnections[connectionId].addIceCandidate(candidate);
//					}
//				}
//			}
//		});
//	}
//
//	public void setRemoteDescription(final SessionDescription sdp, final int connectionId) {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (peerConnections[connectionId] == null || isError) {
//					return;
//				}
//				String sdpDescription = sdp.description;
//				if (preferIsac) {
//					sdpDescription = preferCodec(sdpDescription,
//							AUDIO_CODEC_ISAC, true);
//				}
//				if (videoCallEnabled && preferH264) {
//					sdpDescription = preferCodec(sdpDescription,
//							VIDEO_CODEC_H264, false);
//				}
//				if (videoCallEnabled
//						&& peerConnectionParameters.videoStartBitrate > 0) {
//					sdpDescription = setStartBitrate(VIDEO_CODEC_VP8, true,
//							sdpDescription,
//							peerConnectionParameters.videoStartBitrate);
//					sdpDescription = setStartBitrate(VIDEO_CODEC_VP9, true,
//							sdpDescription,
//							peerConnectionParameters.videoStartBitrate);
//					sdpDescription = setStartBitrate(VIDEO_CODEC_H264, true,
//							sdpDescription,
//							peerConnectionParameters.videoStartBitrate);
//				}
//				if (peerConnectionParameters.audioStartBitrate > 0) {
//					sdpDescription = setStartBitrate(AUDIO_CODEC_OPUS, false,
//							sdpDescription,
//							peerConnectionParameters.audioStartBitrate);
//				}
//				Log.d(TAG, "Set remote SDP.");
//				SessionDescription sdpRemote = new SessionDescription(sdp.type,
//						sdpDescription);
//				peerConnections[connectionId].setRemoteDescription((SdpObserver) sdpObservers[connectionId], sdpRemote);
//			}
//		});
//	}
//
//	public void stopVideoSource() {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoSource != null && !videoSourceStopped) {
//					Log.d(TAG, "Stop video source.");
//					videoSource.stop();
//					videoSourceStopped = true;
//				}
//			}
//		});
//	}
//
//	public void startVideoSource() {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoSource != null && videoSourceStopped) {
//					Log.d(TAG, "Restart video source.");
//					videoSource.restart();
//					videoSourceStopped = false;
//				}
//			}
//		});
//	}
//
//	private void reportError(final String errorMessage) {
//		Log.e(TAG, "Peerconnection error: " + errorMessage);
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				if (!isError) {
//					events.onPeerConnectionError(errorMessage);
//					isError = true;
//				}
//			}
//		});
//	}
//
//	private VideoTrack createVideoTrack() {
//		videoSource = factory.createVideoSource(false);
//
//		localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
//		localVideoTrack.setEnabled(renderVideo);
//		localVideoTrack.addSink(localRender);
//		return localVideoTrack;
//	}
//
//	private void drainCandidates(int connectionId) {
//		if (queuedRemoteCandidateLists[connectionId] != null) {
//			Log.d(TAG, "Add " + queuedRemoteCandidateLists[connectionId].size()
//					+ " remote candidates");
//			for (IceCandidate candidate : queuedRemoteCandidateLists[connectionId]) {
//				peerConnections[connectionId].addIceCandidate(candidate);
//			}
//			queuedRemoteCandidateLists[connectionId] = null;
//		}
//	}
//
//	private void switchCameraInternal() {
//		if (!videoCallEnabled || numberOfCameras < 2 || isError
//				|| videoCapturer == null) {
//			Log.e(TAG, "Failed to switch camera. Video: " + videoCallEnabled
//					+ ". Error : " + isError + ". Number of cameras: "
//					+ numberOfCameras);
//			return; // No video is sent or only one camera is available or error
//			// happened.
//		}
//		Log.d(TAG, "Switch camera");
//		videoCapturer.switchCamera(null);
//	}
//
//	public void switchCamera() {
//		new Handler().post(new Runnable() {
//			@Override
//			public void run() {
//				switchCameraInternal();
//			}
//		});
//	}
//
//	@Override public void onIceCandidate(IceCandidate iceCandidate) {
//
//	}
//
//	@Override public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
//
//	}
//
//	@Override public void onAudioDeviceChanged(AppRTCAudioManager.AudioDevice selectedAudioDevice,
//	                                           Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
//
//	}
//
//	/**
//	 * Peer connection events.
//	 */
//	public static interface PeerConnectionEvents {
//		/**
//		 * Callback fired once local SDP is created and set.
//		 */
//		public void onLocalDescription(final SessionDescription sdp, int connectionId);
//
//		/**
//		 * Callback fired once local Ice candidate is generated.
//		 */
//		public void onIceCandidate(final IceCandidate candidate, int connectionId);
//
//		/**
//		 * Callback fired once connection is established (IceConnectionState is
//		 * CONNECTED).
//		 */
//		public void onIceConnected(int connectionId);
//
//		/**
//		 * Callback fired once connection is closed (IceConnectionState is
//		 * DISCONNECTED).
//		 */
//		public void onIceDisconnected(int connectionId);
//
//		/**
//		 * Callback fired once connection fails (IceConnectionState is
//		 * FAILED).
//		 */
//		public void onIceFailed(int connectionId);
//
//		/**
//		 * Callback fired once a peer connection is closed.
//		 */
//		public void onPeerConnectionClosed(int connectionId);
//
//		/**
//		 * Callback fired once all peer connections are closed.
//		 */
//		void onAllPeerConnectionsClosed();
//
//		/**
//		 * Callback fired once peer connection statistics is ready.
//		 */
//		void onPeerConnectionStatsReady(final StatsReport[] reports);
//
//		/**
//		 * Callback fired once peer connection error happened.
//		 */
//		public void onPeerConnectionError(final String description);
//	}
//
//	/**
//	 * Peer connection parameters.
//	 */
//	public static class PeerConnectionParameters {
//		public final boolean videoCallEnabled;
//		public final boolean loopback;
//		public final int videoWidth;
//		public final int videoHeight;
//		public final int videoFps;
//		public final int videoStartBitrate;
//		public final String videoCodec;
//		public final boolean videoCodecHwAcceleration;
//		public final int audioStartBitrate;
//		public final String audioCodec;
//		public final boolean noAudioProcessing;
//		public final boolean cpuOveruseDetection;
//		public final boolean audioCallEnabled;
//
//		public PeerConnectionParameters(boolean videoCallEnabled,
//		                                boolean loopback, int videoWidth, int videoHeight,
//		                                int videoFps, int videoStartBitrate, String videoCodec,
//		                                boolean videoCodecHwAcceleration, int audioStartBitrate,
//		                                String audioCodec, boolean noAudioProcessing,
//		                                boolean cpuOveruseDetection, boolean audioCallEnabled) {
//			this.videoCallEnabled = videoCallEnabled;
//			this.loopback = loopback;
//			this.videoWidth = videoWidth;
//			this.videoHeight = videoHeight;
//			this.videoFps = videoFps;
//			this.videoStartBitrate = videoStartBitrate;
//			this.videoCodec = videoCodec;
//			this.videoCodecHwAcceleration = videoCodecHwAcceleration;
//			this.audioStartBitrate = audioStartBitrate;
//			this.audioCodec = audioCodec;
//			this.noAudioProcessing = noAudioProcessing;
//			this.cpuOveruseDetection = cpuOveruseDetection;
//			this.audioCallEnabled = audioCallEnabled;
//		}
//	}
//
//	// Implementation detail: observe ICE & stream changes and react
//	// accordingly.
//	private class PCObserver implements PeerConnection.Observer {
//		int connectionId;
//
//		@Override
//		public void onIceCandidate(final IceCandidate candidate) {
//			new Handler().post(new Runnable() {
//				@Override
//				public void run() {
//					events.onIceCandidate(candidate, connectionId);
//				}
//			});
//		}
//
//		@Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
//
//		}
//
//		@Override
//		public void onSignalingChange(PeerConnection.SignalingState newState) {
//			Log.d(TAG, "SignalingState: " + newState);
//		}
//
//		@Override
//		public void onIceConnectionChange(
//				final PeerConnection.IceConnectionState newState) {
//			new Handler().post(new Runnable() {
//				@Override
//				public void run() {
//					Log.d(TAG, "IceConnectionState: " + newState);
//					if (newState == PeerConnection.IceConnectionState.CONNECTED) {
//						events.onIceConnected(connectionId);
//					} else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
//						events.onIceDisconnected(connectionId);
//					} else if (newState == PeerConnection.IceConnectionState.FAILED) {
//						// reportError("ICE connection failed.");
//						events.onIceFailed(connectionId);
//					}
//				}
//			});
//		}
//
//		@Override
//		public void onIceGatheringChange(
//				PeerConnection.IceGatheringState newState) {
//			Log.d(TAG, "IceGatheringState: " + newState);
//		}
//
//		@Override
//		public void onIceConnectionReceivingChange(boolean receiving) {
//			Log.d(TAG, "IceConnectionReceiving changed to " + receiving);
//		}
//
//		@Override
//		public void onAddStream(final MediaStream stream) {
//			new Handler().post(new Runnable() {
//				@Override
//				public void run() {
//					if (peerConnections[connectionId] == null || isError) {
//						return;
//					}
//					if (stream.audioTracks.size() > 1
//							|| stream.videoTracks.size() > 1) {
//						reportError("Weird-looking stream: " + stream);
//						return;
//					}
//					if (stream.videoTracks.size() == 1) {
//						remoteVideoTracks[connectionId] = stream.videoTracks.get(0);
//						remoteVideoTracks[connectionId].setEnabled(renderVideo);
//						remoteVideoTracks[connectionId].addSink(
//								remoteRenders[connectionId]);
//					}
//				}
//			});
//		}
//
//		@Override
//		public void onRemoveStream(final MediaStream stream) {
//			new Handler().post(new Runnable() {
//				@Override
//				public void run() {
//					if (peerConnections[connectionId] == null || isError) {
//						return;
//					}
//					Log.d(TAG, "onRemoveStream called " + connectionId);
//					remoteVideoTracks[connectionId] = null;
//					stream.videoTracks.get(0).dispose();
//				}
//			});
//		}
//
//		@Override
//		public void onDataChannel(final DataChannel dc) {
//			reportError("AppRTC doesn't use data channels, but got: "
//					+ dc.label() + " anyway!");
//		}
//
//		@Override
//		public void onRenegotiationNeeded() {
//			// No need to do anything; AppRTC follows a pre-agreed-upon
//			// signaling/negotiation protocol.
//		}
//
//		@Override public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
//
//		}
//	}
//
//	// Implementation detail: handle offer creation/signaling and answer
//	// setting,
//	// as well as adding remote ICE candidates once the answer SDP is set.
//	private class SDPObserver implements SdpObserver {
//		int connectionId;
//
//		@Override
//		public void onCreateSuccess(final SessionDescription origSdp) {
//			if (localSdps[connectionId] != null) {
//				reportError("Multiple SDP create.");
//				return;
//			}
//			String sdpDescription = origSdp.description;
//			if (preferIsac) {
//				sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC,
//						true);
//			}
//			if (videoCallEnabled && preferH264) {
//				sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_H264,
//						false);
//			}
//			final SessionDescription sdp = new SessionDescription(origSdp.type,
//					sdpDescription);
//			localSdps[connectionId] = sdp;
//			new Handler().post(new Runnable() {
//				@Override
//				public void run() {
//					if (peerConnections[connectionId] != null && !isError) {
//						Log.d(TAG, "Set local SDP from " + sdp.type);
//						peerConnections[connectionId].setLocalDescription(sdpObservers[connectionId], sdp);
//					}
//				}
//			});
//		}
//
//		@Override
//		public void onSetSuccess() {
//			new Handler().post(new Runnable() {
//				@Override
//				public void run() {
//					if (peerConnections[connectionId] == null || isError) {
//						return;
//					}
//					if (isConnectionInitiator[connectionId]) {
//						// For offering peer connection we first create offer
//						// and set
//						// local SDP, then after receiving answer set remote
//						// SDP.
//						if (peerConnections[connectionId].getRemoteDescription() == null) {
//							// We've just set our local SDP so time to send it.
//							Log.d(TAG, "Local SDP set succesfully");
//							events.onLocalDescription(localSdps[connectionId], connectionId);
//						} else {
//							// We've just set remote description, so drain
//							// remote
//							// and send local ICE candidates.
//							Log.d(TAG, "Remote SDP set succesfully");
//							drainCandidates(connectionId);
//						}
//					} else {
//						// For answering peer connection we set remote SDP and
//						// then
//						// create answer and set local SDP.
//						if (peerConnections[connectionId].getLocalDescription() != null) {
//							// We've just set our local SDP so time to send it,
//							// drain
//							// remote and send local ICE candidates.
//							Log.d(TAG, "Local SDP set succesfully");
//							events.onLocalDescription(localSdps[connectionId], connectionId);
//							drainCandidates(connectionId);
//						} else {
//							// We've just set remote SDP - do nothing for now -
//							// answer will be created soon.
//							Log.d(TAG, "Remote SDP set succesfully");
//						}
//					}
//				}
//			});
//		}
//
//		@Override
//		public void onCreateFailure(final String error) {
//			reportError("createSDP error: " + error);
//		}
//
//		@Override
//		public void onSetFailure(final String error) {
//			reportError("setSDP error: " + error);
//		}
//	}
//}
