using UnityEngine;

namespace PoC
{
    public class CharacterBridge : MonoBehaviour
    {
        [SerializeField] private Animator animator;

        // Android에서 UnitySendMessage로 호출
        void PlayEmotion(string emotionName)
        {
            Debug.Log($"[PoC] PlayEmotion: {emotionName}");
            animator.SetTrigger(emotionName);
        }

        void OnUnityLoaded(string _)
        {
            Debug.Log("[PoC] Unity loaded and ready");
        }
    }
}